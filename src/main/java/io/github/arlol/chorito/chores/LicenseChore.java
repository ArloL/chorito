package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.time.Year;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;

public class LicenseChore implements Chore {

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

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path license = context.resolve("LICENSE");
		moveIfExists(context.resolve("LICENSE.md"), license);
		moveIfExists(context.resolve("LICENSE.txt"), license);

		if (!isGitHubProject(context)) {
			return context;
		}
		checkPom(context);

		String currentYear = "" + Year.now(context.clock()).getValue();
		if (FilesSilent.exists(license)) {
			updateCopyrightYear(license, currentYear);
		} else {
			FilesSilent.writeString(
					license,
					MIT_LICENSE.replace("${YEAR}", currentYear)
			);
		}
		return context;
	}

	private static void moveIfExists(Path source, Path target) {
		if (FilesSilent.exists(source)) {
			FilesSilent.move(source, target);
		}
	}

	private static boolean isGitHubProject(ChoreContext context) {
		return context.remotes()
				.stream()
				.anyMatch(s -> s.startsWith("https://github.com"));
	}

	private void updateCopyrightYear(Path license, String currentYear) {
		String content = FilesSilent.readString(license);
		Optional<String> existingRange = readYearRangeFromFile(content);
		if (existingRange.isEmpty()) {
			return;
		}
		String range = existingRange.orElseThrow();
		FilesSilent.writeString(
				license,
				content.replace(range, endYearRangeAt(range, currentYear))
		);
	}

	/**
	 * Keeps the start of an existing copyright range and moves its end to the
	 * current year, collapsing it to a single year when they are the same.
	 */
	private static String endYearRangeAt(String range, String currentYear) {
		String startYear = range.contains("-") ? range.split("-")[0] : range;
		if (startYear.equals(currentYear)) {
			return startYear;
		}
		return startYear + "-" + currentYear;
	}

	private void checkPom(ChoreContext context) {
		DirectoryStreams.rootMavenPoms(context).forEach(pomXml -> {
			if (!FilesSilent.readString(pomXml).contains("licenses")) {
				System.out.println("Add license to pom: " + pomXml);
			}
		});
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
