package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.JsonMigrations.replaceString;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class JsonCommentsTest {

	@Test
	public void keepsALeadingCommentOnAMember() {
		var content = """
				{
				    // why a is here
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsSeveralLeadingCommentLinesOnAMember() {
		var content = """
				{
				    // why a is here
				    // and a second line about it
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsATrailingCommentOnAMember() {
		var content = """
				{
				    "a": "1", // why a is here
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsATrailingCommentOnTheLastMember() {
		var content = """
				{
				    "a": "1",
				    "b": "2", // why b is here
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsATrailingCommentAfterANestedObject() {
		var content = """
				{
				    "a": {
				        "b": "1",
				    }, // why a is here
				    "c": "2",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsACommentAboveTheFirstMember() {
		var content = """
				{
				    // above the first
				    "a": "1",
				    "b": "2",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsACommentOnANestedMember() {
		var content = """
				{
				    "a": {
				        // why b is here
				        "b": "1",
				    },
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsACommentOnAMemberOfAnObjectInsideAnArray() {
		var content = """
				{
				    "a": [
				        {
				            // why b is here
				            "b": "1",
				        },
				    ],
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsAHeaderComment() {
		var content = """
				// what this file is for
				{
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsABlockComment() {
		var content = """
				{
				    /*
				     * why a is here
				     */
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void keepsASingleLineBlockComment() {
		var content = """
				{
				    /* why a is here */
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void doesNotReadACommentOutOfAString() {
		var content = """
				{
				    "a": "https://example.com/x",
				    "b": "/* not a comment */",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

	@Test
	public void movesACommentWithItsMemberWhenTheFileIsSorted() {
		var content = """
				{
				    "b": "2",
				    // why a is here
				    "a": "1",
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo("""
				{
				    // why a is here
				    "a": "1",
				    "b": "2",
				}
				""");
	}

	@Test
	public void keepsACommentOnTheKeyAMigrationRewrites() {
		var content = """
				{
				    // keep me
				    "minimumReleaseAge": "4 days",
				}
				""";

		var result = JsonBuilder.wrap(content)
				.apply(
						List.of(
								replaceString(
										"minimumReleaseAge",
										"4 days",
										"7 days"
								)
						)
				)
				.asString();

		assertThat(result).isEqualTo("""
				{
				    // keep me
				    "minimumReleaseAge": "7 days",
				}
				""");
	}

	@Test
	public void keepsCommentsOnSeveralMembersWhenAnUnrelatedKeyChanges() {
		var content = """
				// what this file is for
				{
				    // why a is here
				    "a": "1", // and a trailing note
				    "minimumReleaseAge": "4 days",
				    /*
				     * why z is here
				     */
				    "z": {
				        // why y is here
				        "y": "2",
				    },
				}
				""";

		var result = JsonBuilder.wrap(content)
				.apply(
						List.of(
								replaceString(
										"minimumReleaseAge",
										"4 days",
										"7 days"
								)
						)
				)
				.asString();

		assertThat(result).isEqualTo("""
				// what this file is for
				{
				    // why a is here
				    "a": "1", // and a trailing note
				    "minimumReleaseAge": "7 days",
				    /*
				     * why z is here
				     */
				    "z": {
				        // why y is here
				        "y": "2",
				    },
				}
				""");
	}

	@Test
	public void attachesACommentToAnEntryItAdds() {
		var result = JsonBuilder.object()
				.array("customManagers", "regex")
				.comment(
						"customManagers",
						"Version pins that no built-in manager sees."
				)
				.asString();

		assertThat(result).isEqualTo("""
				{
				    // Version pins that no built-in manager sees.
				    "customManagers": [
				        "regex",
				    ],
				}
				""");
	}

	@Test
	public void attachesAMultiLineCommentToAnEntryItAdds() {
		var result = JsonBuilder.object()
				.put("a", "1")
				.comment("a", "first line\nsecond line")
				.asString();

		assertThat(result).isEqualTo("""
				{
				    // first line
				    // second line
				    "a": "1",
				}
				""");
	}

	@Test
	public void attachesACommentToAMemberOfANestedObject() {
		var result = JsonBuilder.object()
				.object("a", a -> a.put("b", "1").comment("b", "why b"))
				.asString();

		assertThat(result).isEqualTo("""
				{
				    "a": {
				        // why b
				        "b": "1",
				    },
				}
				""");
	}

	// Comments are attached to object members. One sitting between array
	// elements has no member to hang on and is dropped.
	@Test
	public void dropsACommentBetweenArrayElements() {
		var content = """
				{
				    "a": [
				        // dropped
				        "x",
				    ],
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo("""
				{
				    "a": [
				        "x",
				    ],
				}
				""");
	}

	// Same reason: a comment after the last member has no following member to
	// lead and is dropped.
	@Test
	public void dropsACommentAfterTheLastMember() {
		var content = """
				{
				    "a": "1",
				    // dropped
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo("""
				{
				    "a": "1",
				}
				""");
	}

	@Test
	public void leavesAFileWithoutCommentsAlone() {
		var content = """
				{
				    "a": [
				        "1",
				    ],
				    "b": {
				        "c": "2",
				    },
				    "d": true,
				    "e": 1,
				    "f": null,
				}
				""";

		assertThat(JsonBuilder.wrap(content).asString()).isEqualTo(content);
	}

}
