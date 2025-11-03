package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class RenovateChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path renovateJson = context.resolve("renovate.json");
		if (!FilesSilent.exists(renovateJson)) {
			FilesSilent.writeString(
					renovateJson,
					"""
							{
							  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
							  "extends": [
							    "config:recommended"
							  ],
							  "minimumReleaseAge": "4 days"
							}
							"""
			);
			context.setDirty();
		}
		return context;
	}

}
