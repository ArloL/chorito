package io.github.arlol.chorito.tools;

import java.util.random.RandomGenerator;

public class RandomCronBuilder {

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

	public String randomDayOfMonth() {
		return minute().hour().dayOfMonth().build();
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
