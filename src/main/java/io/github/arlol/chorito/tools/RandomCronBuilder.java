package io.github.arlol.chorito.tools;

import java.util.Random;

public class RandomCronBuilder {

	private final Random random = new Random();

	private final String minute;
	private final String hour;
	private final String dayOfMonth;
	private final String month;
	private final String dayOfWeek;

	public static String randomDayOfMonth() {
		return new RandomCronBuilder().minute().hour().dayOfMonth().build();
	}

	public RandomCronBuilder() {
		this("*", "*", "*", "*", "*");
	}

	public RandomCronBuilder(
			String minute,
			String hour,
			String dayOfMonth,
			String month,
			String dayOfWeek
	) {
		this.minute = minute;
		this.hour = hour;
		this.dayOfMonth = dayOfMonth;
		this.month = month;
		this.dayOfWeek = dayOfWeek;
	}

	public RandomCronBuilder minute() {
		String minute = "" + random.nextInt(1, 55 + 1);
		return new RandomCronBuilder(
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
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder dayOfMonth() {
		String dayOfWeek = "" + random.nextInt(1, 28 + 1);
		return new RandomCronBuilder(
				minute,
				hour,
				dayOfMonth,
				month,
				dayOfWeek
		);
	}

	public RandomCronBuilder month() {
		String dayOfWeek = "" + random.nextInt(1, 12 + 1);
		return new RandomCronBuilder(
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
