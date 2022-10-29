package io.github.arlol.chorito.tools;

import java.util.random.RandomGenerator;

public class FakeRandomGenerator implements RandomGenerator {

	public FakeRandomGenerator() {
	}

	@Override
	public long nextLong() {
		return 5;
	}

}
