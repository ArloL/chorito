package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class LicenseChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new LicenseChore().doit(extension.choreContext());
	}

	@Test
	public void testCreate() throws Exception {
		Instant instant = Instant.parse("2018-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		Path license = extension.root().resolve("LICENSE");
		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2018"));
	}

	@Test
	public void testUpdateLicenseMd() throws Exception {
		Path licenseMd = extension.root().resolve("LICENSE.md");
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(licenseMd, mitLicense("2018"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertFalse(FilesSilent.exists(licenseMd));
		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testUpdate() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testUpdateSameYear() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018"));

		Instant instant = Instant.parse("2018-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2018"));
	}

	@Test
	public void testDontTouchExistingRange() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018-2019"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testDontTouchDifferentLicense() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, gnuAfferoLicense());

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(gnuAfferoLicense());
	}

	@Test
	public void testUpdateExistingRange() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2017-2018"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content().isEqualTo(mitLicense("2017-2019"));
	}

	@Test
	public void testMultipleAuthors() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(
				license,
				mitLicense("2017-2018", "2012 Ryan Bates")
		);

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore().doit(context);

		assertTrue(FilesSilent.exists(license));
		assertThat(license).content()
				.isEqualTo(mitLicense("2017-2019", "2012 Ryan Bates"));
	}

	private String mitLicense(String yearRange) {
		return LicenseChore.MIT_LICENSE.replace("${YEAR}", yearRange);
	}

	private String mitLicense(String yearRange, String author) {
		return LicenseChore.MIT_LICENSE.replace("${YEAR}", yearRange)
				.replace("Keeffe\n", "Keeffe\nCopyright (c) " + author + "\n");
	}

	private String gnuAfferoLicense() {
		return """
				                    GNU AFFERO GENERAL PUBLIC LICENSE
				                       Version 3, 19 November 2007

				 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
				 Everyone is permitted to copy and distribute verbatim copies
				 of this license document, but changing it is not allowed.

				                            Preamble

				  The GNU Affero General Public License is a free, copyleft license for
				software and other kinds of works, specifically designed to ensure
				cooperation with the community in the case of network server software.

				  The licenses for most software and other practical works are designed
				to take away your freedom to share and change the works.  By contrast,
				our General Public Licenses are intended to guarantee your freedom to
				share and change all versions of a program--to make sure it remains free
				software for all its users.
				""";

	}

}
