package se.ohdeere.nightscout.plugin.ar2;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-math tests for {@link Ar2Plugin#predict(double, double)}. The model is a linear
 * AR(2) extrapolation: each forecast point is {@code 2 * curr - prev}, sliding the window
 * forward. Six points = 30 minutes at 5 min/point.
 */
class Ar2PluginTests {

	@Test
	void linearRiseExtrapolatesEvenly() {
		// y1=100, y2=110 → next = 2*110 - 100 = 120, then 130, 140, 150, 160, 170
		List<Double> forecast = Ar2Plugin.predict(100, 110);
		assertThat(forecast).containsExactly(120.0, 130.0, 140.0, 150.0, 160.0, 170.0);
	}

	@Test
	void linearFallExtrapolatesEvenly() {
		List<Double> forecast = Ar2Plugin.predict(180, 170);
		assertThat(forecast).containsExactly(160.0, 150.0, 140.0, 130.0, 120.0, 110.0);
	}

	@Test
	void flatTrendStaysFlat() {
		List<Double> forecast = Ar2Plugin.predict(120, 120);
		assertThat(forecast).hasSize(6).allSatisfy(v -> assertThat(v).isEqualTo(120.0));
	}

	@Test
	void forecastHasSixPoints() {
		assertThat(Ar2Plugin.predict(100, 105)).hasSize(6);
	}

}
