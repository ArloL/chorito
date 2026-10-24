package io.github.arlol.chorito.tools;

import java.util.List;
import java.util.function.Consumer;

public final class JsonMigrations {

	private JsonMigrations() {
	}

	public static JsonMigration replaceString(
			String key,
			String from,
			String to
	) {
		return builder -> builder.migrateString(key, from, to);
	}

	public static JsonMigration replaceArray(
			String key,
			List<String> from,
			List<String> to
	) {
		return builder -> from.equals(builder.arrayStrings(key))
				? builder.array(key, to)
				: builder;
	}

	public static JsonMigration ifAbsent(
			String key,
			Consumer<JsonBuilder> body
	) {
		return builder -> builder.ifAbsent(key, body);
	}

	public static JsonMigration whenObject(String key, JsonMigration... inner) {
		return builder -> builder.ifObjectPresent(key, sub -> {
			for (JsonMigration m : inner) {
				m.apply(sub);
			}
		});
	}

}
