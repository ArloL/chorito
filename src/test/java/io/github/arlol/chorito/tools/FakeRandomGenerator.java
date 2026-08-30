package io.github.arlol.chorito.tools;

import java.util.random.RandomGenerator;

public class FakeRandomGenerator implements RandomGenerator {

	@Override
	public long nextLong() {
		return 5;
	}

}
