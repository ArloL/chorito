package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

public class YamlPathTest {

	@Test
	void name() throws Exception {
		// given
		Node input = Yamls.load("""
				hi: lol
				""").orElseThrow();

		// when
		var yamlPath = Yamls.getYamlPath(input, "hi");

		// then
		assertThat(yamlPath).hasSize(1).first().matches(node -> {
			if (node instanceof ScalarNode scalar) {
				return scalar.getValue().equals("lol");
			}
			return false;
		});
	}

	@Test
	void updates() throws Exception {
		// given
		Node input = Yamls.load("""
				updates:
				- ecosystem: docker
				- ecosystem: maven
				""").orElseThrow();

		// when
		var yamlPath = Yamls.getYamlPath(input, "updates/ecosystem");

		// then
		assertThat(
				yamlPath.stream()
						.map(node -> (ScalarNode) node)
						.map(ScalarNode::getValue)
						.toList()
		).contains("docker", "maven");
	}

	@Test
	void updatesOne() throws Exception {
		// given
		Node input = Yamls.load("""
				updates:
				- ecosystem: docker
				- ecosystem: maven
				""").orElseThrow();

		// when
		var yamlPath = Yamls.getYamlPath(input, "updates/1");

		// then
		assertThat(yamlPath).hasSize(1);
		assertThat(
				Yamls.getKeyAsScalar(
						Yamls.nodeAsMap(yamlPath.getFirst()),
						"ecosystem"
				).orElseThrow().getValue()
		).isEqualTo("maven");
	}

	@Test
	void updatesAttribute() throws Exception {
		// given
		Node input = Yamls.load("""
				updates:
				- ecosystem: docker
				- ecosystem: maven
				""").orElseThrow();

		// when
		var yamlPath = Yamls.getYamlPath(input, "updates[ecosystem=maven]");

		// then
		assertThat(yamlPath).hasSize(1);
		assertThat(
				Yamls.getKeyAsScalar(
						Yamls.nodeAsMap(yamlPath.getFirst()),
						"ecosystem"
				).orElseThrow().getValue()
		).isEqualTo("maven");
	}

}
