package se.ohdeere.nightscout.service.agp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import se.ohdeere.nightscout.service.agp.AgpService.AgpBucket;
import se.ohdeere.nightscout.storage.entries.Entry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-math tests for {@link AgpService#compute(List, int, int)} and
 * {@link AgpService#percentile(List, double)}. No Spring, no DB.
 */
class AgpServiceTests {

	/* ---------- percentile() ---------- */

	@Test
	void percentileMatchesPostgresPercentileCont() {
		// Linear interpolation: for [10, 20, 30, 40, 50], p25 = 20, p50 = 30, p75 = 40
		List<Integer> sorted = Arrays.asList(10, 20, 30, 40, 50);
		assertThat(AgpService.percentile(sorted, 0.0)).isEqualTo(10.0);
		assertThat(AgpService.percentile(sorted, 0.25)).isEqualTo(20.0);
		assertThat(AgpService.percentile(sorted, 0.5)).isEqualTo(30.0);
		assertThat(AgpService.percentile(sorted, 0.75)).isEqualTo(40.0);
		assertThat(AgpService.percentile(sorted, 1.0)).isEqualTo(50.0);
	}

	@Test
	void percentileInterpolatesBetweenSamples() {
		// [100, 200, 300, 400] → p50 sits between 200 and 300 at index 1.5 → 250
		List<Integer> sorted = Arrays.asList(100, 200, 300, 400);
		assertThat(AgpService.percentile(sorted, 0.5)).isEqualTo(250.0);
	}

	@Test
	void percentileHandlesSingleAndEmpty() {
		assertThat(AgpService.percentile(List.of(120), 0.5)).isEqualTo(120.0);
		assertThat(AgpService.percentile(List.of(), 0.5)).isEqualTo(0.0);
	}

	/* ---------- compute() ---------- */

	@Test
	void computeBucketsByTimeOfDay() {
		// Two readings at the same minute-of-day across different days fall into the
		// same bucket. Reading 1: 2026-01-01 06:00 UTC. Reading 2: 2026-01-02 06:00 UTC.
		long dayMs = 24L * 60 * 60 * 1000;
		long t1 = Instant.parse("2026-01-01T06:00:00Z").toEpochMilli();
		long t2 = t1 + dayMs;
		long t3 = t1 + 2 * dayMs;
		List<Entry> entries = Arrays.asList(entry(t1, 100), entry(t2, 120), entry(t3, 110));

		List<AgpBucket> buckets = AgpService.compute(entries, 15, 0);

		assertThat(buckets).hasSize(1);
		AgpBucket b = buckets.get(0);
		assertThat(b.bucketMinute()).isEqualTo(6 * 60); // 06:00 = 360 min
		assertThat(b.count()).isEqualTo(3);
		assertThat(b.p50()).isEqualTo(110.0);
		assertThat(b.p5()).isEqualTo(101.0); // linear interp between 100 and 120
		assertThat(b.p95()).isEqualTo(119.0);
	}

	@Test
	void computeAppliesTimezoneOffset() {
		// 06:00 UTC with +120 offset (CEST) is 08:00 local → bucket 32 (480 min).
		long t = Instant.parse("2026-01-01T06:00:00Z").toEpochMilli();
		List<AgpBucket> buckets = AgpService.compute(List.of(entry(t, 100)), 15, 120);

		assertThat(buckets).hasSize(1);
		assertThat(buckets.get(0).bucketMinute()).isEqualTo(8 * 60);
	}

	@Test
	void computeWrapsNegativeOffsetCorrectly() {
		// 01:00 UTC with -120 offset (e.g. somewhere in the Atlantic) is 23:00
		// the previous day → bucket should land at 1380 minutes (23:00), not negative.
		long t = Instant.parse("2026-01-01T01:00:00Z").toEpochMilli();
		List<AgpBucket> buckets = AgpService.compute(List.of(entry(t, 100)), 15, -120);

		assertThat(buckets).hasSize(1);
		assertThat(buckets.get(0).bucketMinute()).isEqualTo(23 * 60);
	}

	@Test
	void computeSkipsEmptyBuckets() {
		// One reading at 06:00 → only one bucket in the result, not 96.
		long t = Instant.parse("2026-01-01T06:00:00Z").toEpochMilli();
		List<AgpBucket> buckets = AgpService.compute(List.of(entry(t, 100)), 15, 0);
		assertThat(buckets).hasSize(1);
	}

	@Test
	void computeIgnoresNullSgv() {
		long t = Instant.parse("2026-01-01T06:00:00Z").toEpochMilli();
		List<Entry> entries = new ArrayList<>();
		entries.add(entry(t, null));
		entries.add(entry(t, 100));
		List<AgpBucket> buckets = AgpService.compute(entries, 15, 0);
		assertThat(buckets).hasSize(1);
		assertThat(buckets.get(0).count()).isEqualTo(1);
	}

	@Test
	void computeRejectsBadBucketSize() {
		try {
			AgpService.compute(List.of(), 7, 0);
			assertThat(false).isTrue();
		}
		catch (IllegalArgumentException expected) {
			// 1440 % 7 != 0
		}
	}

	@Test
	void computeReturnsBucketsInOrder() {
		// Three readings at 06:00, 12:00, 18:00 → three sorted buckets.
		long t = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		List<Entry> entries = Arrays.asList(entry(t + 6 * 3600 * 1000L, 100), entry(t + 12 * 3600 * 1000L, 110),
				entry(t + 18 * 3600 * 1000L, 120));
		List<AgpBucket> buckets = AgpService.compute(entries, 15, 0);
		assertThat(buckets).extracting(AgpBucket::bucketMinute).containsExactly(6 * 60, 12 * 60, 18 * 60);
	}

	private static Entry entry(long dateMs, Integer sgv) {
		String iso = Instant.ofEpochMilli(dateMs).toString();
		return new Entry(UUID.randomUUID(), "sgv", dateMs, iso, iso, sgv, "Flat", null, null, null, null, "test", 0,
				Instant.now());
	}

}
