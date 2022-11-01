package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.time.Year;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class LicenseChore {

	private static final String MIT_LICENSE = """
			MIT License

			Copyright (c) ${YEAR} Arlo O'Keeffe

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

	private final ChoreContext context;

	public LicenseChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		if (context.hasGitHubRemote()) {
			Path license = context.resolve("LICENSE");
			final String currentYear = ""
					+ Year.now(context.clock()).getValue();
			String newYear = currentYear;
			if (FilesSilent.exists(license)) {
				newYear = readYearFromFile(FilesSilent.readString(license))
						.map(existingYear -> {
							if (existingYear.equals(currentYear)) {
								return currentYear;
							} else {
								return existingYear + "-" + currentYear;
							}
						})
						.orElse(currentYear);
			}
			FilesSilent.writeString(
					license,
					MIT_LICENSE.replace("${YEAR}", newYear)
			);
		}
	}

	private Optional<String> readYearFromFile(String license) {
		String startString = "(c) ";
		int indexOf = license.indexOf(startString);
		if (indexOf == -1) {
			return Optional.empty();
		}
		license = license.substring(indexOf + startString.length());
		indexOf = license.indexOf(" ");
		if (indexOf == -1) {
			return Optional.empty();
		}
		return Optional.of(license.substring(0, indexOf));
	}

}
