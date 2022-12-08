package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.time.Year;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class LicenseChore {

	public static final String MIT_LICENSE = """
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
		Path licenseMd = context.resolve("LICENSE.md");
		Path license = context.resolve("LICENSE");
		if (FilesSilent.exists(licenseMd)) {
			FilesSilent.move(licenseMd, license);
		}
		if (context.hasGitHubRemote()) {
			checkPom();
			final String currentYear = ""
					+ Year.now(context.clock()).getValue();
			String newLicenseContent = MIT_LICENSE
					.replace("${YEAR}", currentYear);
			if (FilesSilent.exists(license)) {
				String currentLicenseContent = FilesSilent.readString(license);
				Optional<String> currentRange = readYearRangeFromFile(
						currentLicenseContent
				);
				if (currentRange.isPresent()) {
					String newRange;
					String existingRange = currentRange.get();
					String startYear;
					String endYear;
					if (existingRange.contains("-")) {
						String[] split = existingRange.split("-");
						startYear = split[0];
						endYear = split[1];
					} else {
						startYear = existingRange;
						endYear = existingRange;
					}
					if (!endYear.equals(currentYear)) {
						endYear = currentYear;
					}
					if (startYear.equals(endYear)) {
						newRange = startYear;
					} else {
						newRange = startYear + "-" + endYear;
					}
					newLicenseContent = currentLicenseContent
							.replace(existingRange, newRange);
				}
			}
			FilesSilent.writeString(license, newLicenseContent);
		}
	}

	private void checkPom() {
		Path pomXml = context.resolve("pom.xml");
		if (!FilesSilent.exists(pomXml)) {
			return;
		}
		if (!FilesSilent.readString(pomXml).contains("licenses")) {
			throw new IllegalStateException("Add license to pom");
		}
	}

	private Optional<String> readYearRangeFromFile(String license) {
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
