package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.PathChoreContext;

public class LicenseChoreTest {

	private static final String EXPECTED = """
			MIT License

			Copyright (c) 2018 Arlo O'Keeffe

			Permission is hereby granted, free of charge, to any person obtaining a copy
			of this software and associated documentation files (the "Software"), to deal
			in the Software without restriction, including without limitation the rights
			to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
			copies of the Software, and to permit persons to whom the Software is
			furnished to do so, subject to the following conditions:

			The above copyright notice and this permission notice shall be included in all
			copies or substantial portions of the Software.

			THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
			IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
			FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
			AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
			LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
			OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
			SOFTWARE.
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new LicenseChore(extension.choreContext()).doit();
	}

	@Test
	public void testCreate() throws Exception {
		Instant instant = Instant.parse("2018-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = new PathChoreContext(
				extension.choreContext().root(),
				true,
				extension.choreContext().randomGenerator(),
				Clock.fixed(instant, zoneId)
		);

		new LicenseChore(context.refresh()).doit();

		Path license = context.resolve("LICENSE");
		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license)).isEqualTo(EXPECTED);
	}

	@Test
	public void testUpdate() throws Exception {
		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = new PathChoreContext(
				extension.choreContext().root(),
				true,
				extension.choreContext().randomGenerator(),
				Clock.fixed(instant, zoneId)
		);

		Path license = context.resolve("LICENSE");
		FilesSilent.writeString(license, EXPECTED);
		new LicenseChore(context.refresh()).doit();
		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(EXPECTED.replace("2018", "2018-2019"));
	}

	@Test
	public void testUpdateSameYear() throws Exception {
		Instant instant = Instant.parse("2018-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = new PathChoreContext(
				extension.choreContext().root(),
				true,
				extension.choreContext().randomGenerator(),
				Clock.fixed(instant, zoneId)
		);

		Path license = context.resolve("LICENSE");
		FilesSilent.writeString(license, EXPECTED);
		new LicenseChore(context.refresh()).doit();
		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license)).isEqualTo(EXPECTED);
	}

}
