package se.ohdeere.nightscout.plugin.iob;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-math tests for {@link IobPlugin#bilinearIob(double, double, double)}. Reference
 * values come from the bilinear curve definition itself — these lock in the shape so we
 * notice any drift if the curve is ever changed.
 *
 * <p>
 * For DIA = 180 min the peak sits at 60 min. The pre-peak branch is iob = insulin * (1 -
 * 0.5 * x) with x = t/peak; the post-peak branch is iob = 0.5 * insulin * (1 - x) with x
 * = (t - peak) / (DIA - peak).
 */
class IobPluginTests {

	private static final double DIA = 180.0;

	private static final double INSULIN = 1.0;

	@Test
	void zeroMinutesAgoYieldsFullInsulin() {
		double[] r = IobPlugin.bilinearIob(INSULIN, 0, DIA);
		assertThat(r[0]).isCloseTo(1.0, within(0.0001));
		assertThat(r[1]).isCloseTo(0.0, within(0.0001));
	}

	@Test
	void atPeakIobIsHalf() {
		// At t = peak (60 min), pre-peak branch gives iob = 1 - 0.5 = 0.5
		double[] r = IobPlugin.bilinearIob(INSULIN, 60, DIA);
		assertThat(r[0]).isCloseTo(0.5, within(0.0001));
	}

	@Test
	void midPostPeakIsThreeEighths() {
		// At t = 90 min, x = 30/120 = 0.25 → iob = 0.5 * (1 - 0.25) = 0.375
		double[] r = IobPlugin.bilinearIob(INSULIN, 90, DIA);
		assertThat(r[0]).isCloseTo(0.375, within(0.0001));
	}

	@Test
	void atDiaIobIsZero() {
		double[] r = IobPlugin.bilinearIob(INSULIN, DIA, DIA);
		assertThat(r[0]).isCloseTo(0.0, within(0.0001));
		assertThat(r[1]).isCloseTo(0.0, within(0.0001));
	}

	@Test
	void beyondDiaClampsToZero() {
		double[] r = IobPlugin.bilinearIob(INSULIN, 200, DIA);
		assertThat(r[0]).isEqualTo(0.0);
		assertThat(r[1]).isEqualTo(0.0);
	}

	@Test
	void negativeMinutesClampsToZero() {
		double[] r = IobPlugin.bilinearIob(INSULIN, -5, DIA);
		assertThat(r[0]).isEqualTo(0.0);
		assertThat(r[1]).isEqualTo(0.0);
	}

	@Test
	void scalesLinearlyWithInsulinDose() {
		double[] one = IobPlugin.bilinearIob(1.0, 30, DIA);
		double[] three = IobPlugin.bilinearIob(3.0, 30, DIA);
		assertThat(three[0]).isCloseTo(one[0] * 3.0, within(0.0001));
		assertThat(three[1]).isCloseTo(one[1] * 3.0, within(0.0001));
	}

	private static org.assertj.core.data.Offset<Double> within(double tolerance) {
		return org.assertj.core.data.Offset.offset(tolerance);
	}

}
