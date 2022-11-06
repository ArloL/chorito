package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

public class LicenseChoreTest {

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
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		Path license = extension.root().resolve("LICENSE");
		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2018"));
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
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertFalse(FilesSilent.exists(licenseMd));
		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testUpdate() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testUpdateSameYear() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018"));

		Instant instant = Instant.parse("2018-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2018"));
	}

	@Test
	public void testDontTouchExistingRange() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2018-2019"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2018-2019"));
	}

	@Test
	public void testUpdateExistingRange() throws Exception {
		Path license = extension.root().resolve("LICENSE");
		FilesSilent.writeString(license, mitLicense("2017-2018"));

		Instant instant = Instant.parse("2019-08-19T16:02:42.00Z");
		ZoneId zoneId = ZoneId.of("Asia/Calcutta");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2017-2019"));
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
				.hasGitHubRemote(true)
				.clock(Clock.fixed(instant, zoneId))
				.build();

		new LicenseChore(context).doit();

		assertTrue(FilesSilent.exists(license));
		assertThat(FilesSilent.readString(license))
				.isEqualTo(mitLicense("2017-2019", "2012 Ryan Bates"));
	}

	private String mitLicense(String yearRange) {
		return LicenseChore.MIT_LICENSE.replace("${YEAR}", yearRange);
	}

	private String mitLicense(String yearRange, String author) {
		return LicenseChore.MIT_LICENSE.replace("${YEAR}", yearRange)
				.replace("Keeffe\n", "Keeffe\nCopyright (c) " + author + "\n");
	}

}
