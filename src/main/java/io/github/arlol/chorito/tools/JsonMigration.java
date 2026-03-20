package io.github.arlol.chorito.tools;

@FunctionalInterface
public interface JsonMigration {

	JsonBuilder apply(JsonBuilder builder);

}
