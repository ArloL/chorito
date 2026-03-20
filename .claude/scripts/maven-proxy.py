#!/usr/bin/env python3
"""
Local proxy that forwards Maven requests to the upstream egress proxy with
Proxy-Authorization header injected. This works around Maven's inability to
authenticate with complex JWT-based proxy credentials.

Reads upstream proxy URL from HTTPS_PROXY environment variable at startup.
"""
import socket
import threading
import base64
import os
import sys
import select

LISTEN_HOST = '127.0.0.1'
LISTEN_PORT = 3128

proxy_url = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy', '')
if not proxy_url:
    print('ERROR: HTTPS_PROXY environment variable not set', file=sys.stderr)
    sys.exit(1)

try:
    without_scheme = proxy_url.split('://', 1)[1]
    creds, hostport = without_scheme.rsplit('@', 1)
    UPSTREAM_HOST, UPSTREAM_PORT_STR = hostport.rsplit(':', 1)
    UPSTREAM_PORT = int(UPSTREAM_PORT_STR)
    PROXY_AUTH = base64.b64encode(creds.encode()).decode()
    print(f'Upstream proxy: {UPSTREAM_HOST}:{UPSTREAM_PORT}', flush=True)
except Exception as e:
    print(f'ERROR parsing HTTPS_PROXY: {e}', file=sys.stderr)
    sys.exit(1)


def forward(src, dst):
    try:
        while True:
            r, _, _ = select.select([src], [], [], 30)
            if not r:
                break
            data = src.recv(4096)
            if not data:
                break
            dst.sendall(data)
    except Exception:
        pass
    finally:
        for s in (src, dst):
            try:
                s.close()
            except Exception:
                pass


def handle_client(client_sock):
    try:
        request = b''
        while b'\r\n\r\n' not in request:
            chunk = client_sock.recv(4096)
            if not chunk:
                return
            request += chunk

        first_line = request.split(b'\r\n', 1)[0].decode('utf-8', errors='replace')
        parts = first_line.split()
        method = parts[0] if parts else ''

        upstream = socket.create_connection((UPSTREAM_HOST, UPSTREAM_PORT), timeout=30)

        if method == 'CONNECT':
            target = parts[1] if len(parts) > 1 else ''
            connect_req = (
                f'CONNECT {target} HTTP/1.1\r\n'
                f'Host: {target}\r\n'
                f'Proxy-Authorization: Basic {PROXY_AUTH}\r\n'
                f'Proxy-Connection: Keep-Alive\r\n'
                f'\r\n'
            ).encode()
            upstream.sendall(connect_req)

            response = b''
            while b'\r\n\r\n' not in response:
                chunk = upstream.recv(4096)
                if not chunk:
                    break
                response += chunk

            client_sock.sendall(response)

            if b'200' in response.split(b'\r\n', 1)[0]:
                t1 = threading.Thread(target=forward, args=(client_sock, upstream), daemon=True)
                t2 = threading.Thread(target=forward, args=(upstream, client_sock), daemon=True)
                t1.start()
                t2.start()
                t1.join()
                t2.join()
            else:
                client_sock.close()
                upstream.close()
        else:
            lines = request.split(b'\r\n')
            new_lines = [lines[0]]
            for line in lines[1:]:
                if line.lower().startswith(b'proxy-authorization:'):
                    continue
                new_lines.append(line)
            new_lines.insert(1, f'Proxy-Authorization: Basic {PROXY_AUTH}'.encode())
            upstream.sendall(b'\r\n'.join(new_lines))

            t1 = threading.Thread(target=forward, args=(upstream, client_sock), daemon=True)
            t2 = threading.Thread(target=forward, args=(client_sock, upstream), daemon=True)
            t1.start()
            t2.start()
            t1.join()
            t2.join()

    except Exception as e:
        print(f'handle_client error: {e}', file=sys.stderr)
    finally:
        try:
            client_sock.close()
        except Exception:
            pass


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((LISTEN_HOST, LISTEN_PORT))
    server.listen(50)
    print(f'Local Maven proxy listening on {LISTEN_HOST}:{LISTEN_PORT}', flush=True)
    sys.stdout.flush()

    while True:
        try:
            client_sock, _ = server.accept()
            t = threading.Thread(target=handle_client, args=(client_sock,), daemon=True)
            t.start()
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f'accept error: {e}', file=sys.stderr)

    server.close()


if __name__ == '__main__':
    main()
