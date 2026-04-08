package se.ohdeere.nightscout.plugin.cob;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-math tests for {@link CobPlugin#cobRemaining(double, double, double)}. Linear
 * absorption at {@code carbsPerHr} g/h, clamped at zero. Mirrors the IOB / AR2 test style
 * — no Spring, no DB, just the curve.
 */
class CobPluginTests {

	private static final double CARBS_PER_HR = 20.0;

	@Test
	void zeroMinutesAgoYieldsFullCarbs() {
		assertThat(CobPlugin.cobRemaining(45, 0, CARBS_PER_HR)).isCloseTo(45.0, within(0.0001));
	}

	@Test
	void halfHourAgoConsumesTenGrams() {
		// 30 min @ 20 g/h = 10 g absorbed → 35 g remaining
		assertThat(CobPlugin.cobRemaining(45, 30, CARBS_PER_HR)).isCloseTo(35.0, within(0.0001));
	}

	@Test
	void oneHourAgoConsumesTwentyGrams() {
		assertThat(CobPlugin.cobRemaining(45, 60, CARBS_PER_HR)).isCloseTo(25.0, within(0.0001));
	}

	@Test
	void fullyAbsorbedClampsToZero() {
		// 45 g at 20 g/h fully absorbs in 135 min; anything beyond stays zero.
		assertThat(CobPlugin.cobRemaining(45, 135, CARBS_PER_HR)).isCloseTo(0.0, within(0.0001));
		assertThat(CobPlugin.cobRemaining(45, 240, CARBS_PER_HR)).isEqualTo(0.0);
	}

	@Test
	void negativeMinutesReturnsFullCarbs() {
		// Future-dated treatment shouldn't subtract anything.
		assertThat(CobPlugin.cobRemaining(45, -10, CARBS_PER_HR)).isEqualTo(45.0);
	}

	@Test
	void respectsCustomAbsorptionRate() {
		// 45 g at 30 g/h → after 30 min, 15 g absorbed → 30 g remaining
		assertThat(CobPlugin.cobRemaining(45, 30, 30.0)).isCloseTo(30.0, within(0.0001));
	}

	private static org.assertj.core.data.Offset<Double> within(double tolerance) {
		return org.assertj.core.data.Offset.offset(tolerance);
	}

}
