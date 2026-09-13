package io.github.arlol.chorito.tools;

import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

public class RandomCronBuilder {

	private static final Pattern RANDOM_DAY_OF_MONTH = Pattern
			.compile("\\d+ \\d+ \\d+ \\* \\*");

	private final RandomGenerator random;

	private final String minute;
	private final String hour;
	private final String dayOfMonth;
	private final String month;
	private final String dayOfWeek;

	public RandomCronBuilder(RandomGenerator random) {
		this(random, "*", "*", "*", "*", "*");
	}

	public RandomCronBuilder(
			RandomGenerator random,
			String minute,
			String hour,
			String dayOfMonth,
			String month,
			String dayOfWeek
	) {
		this.random = random;
		this.minute = minute;
		this.hour = hour;
		this.dayOfMonth = dayOfMonth;
		this.month = month;
		this.dayOfWeek = dayOfWeek;
	}

	/**
	 * A cron firing once a month, at a minute, hour and day picked at random.
	 */
	public String randomDayOfMonth() {
		return minute().hour().dayOfMonth().build();
	}

	/**
	 * Whether {@code cron} already has the shape {@link #randomDayOfMonth()}
	 * produces.
	 * <p>
	 * A chore that randomises a schedule has to recognise one it has already
	 * randomised, or it picks a new time on every run. Before this, the caller
	 * asked whether the cron ended in {@code *} -- true of this shape only
	 * because day-of-week and month are left alone, which is a property of this
	 * class that the caller had no way to know and this class had no reason to
	 * keep. Adding {@link #month()} or {@link #dayOfWeek()} to the line above
	 * would have quietly broken it. Keep the two in step here, where they are
	 * three lines apart.
	 */
	public static boolean isRandomDayOfMonth(String cron) {
		return RANDOM_DAY_OF_MONTH.matcher(cron).matches();
	}

	public RandomCronBuilder minute() {
		String minute = "" + random.nextInt(1, 55 + 1);
		return new RandomCronBuilder(
				random,
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder hour() {
		String hour = "" + random.nextInt(3, 23 + 1);
		return new RandomCronBuilder(
				random,
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder dayOfMonth() {
		String dayOfMonth = "" + random.nextInt(1, 28 + 1);
		return new RandomCronBuilder(
				random,
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder month() {
		String month = "" + random.nextInt(1, 12 + 1);
		return new RandomCronBuilder(
				random,
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder dayOfWeek() {
		String dayOfWeek = "" + random.nextInt(0, 6 + 1);
		return new RandomCronBuilder(
				random,
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public String build() {
		return minute + " " + hour + " " + dayOfMonth + " " + month + " "
				+ dayOfWeek;
	}

}
