package se.ohdeere.nightscout.service.agp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.stereotype.Service;

/**
 * Ambulatory Glucose Profile (AGP) service. Bins SGV readings from the last N days by
 * time-of-day (in the caller's timezone, expressed as a minute-offset from UTC) and
 * returns p5/p25/p50/p75/p95 per bucket.
 *
 * <p>
 * Convention: a 15-minute bucket size yields 96 buckets per 24h. The percentile math is a
 * pure-Java helper ({@link #compute}) so it can be exercised without a database — see
 * {@code AgpServiceTests}.
 *
 * <p>
 * No caching yet; the underlying query reads from a single indexed column
 * ({@code date_ms}) and filters in memory, which is fine at the volumes involved (~4 k
 * entries / 14 days).
 */
@Service
public class AgpService {

	public record AgpBucket(int bucketMinute, double p5, double p25, double p50, double p75, double p95, long count) {
	}

	private final EntryRepository entries;

	public AgpService(EntryRepository entries) {
		this.entries = entries;
	}

	/**
	 * Read SGV entries for the last {@code days} days and compute the AGP profile.
	 * @param days how many days back to consider
	 * @param bucketMinutes bucket size in minutes (must divide 1440)
	 * @param offsetMinutes the caller's UTC offset in minutes (e.g. {@code 120} for CEST)
	 */
	public List<AgpBucket> getAgp(int days, int bucketMinutes, int offsetMinutes) {
		if (days <= 0 || days > 90) {
			days = 14;
		}
		if (bucketMinutes <= 0 || 1440 % bucketMinutes != 0) {
			bucketMinutes = 15;
		}
		long sinceMs = Instant.now().minus(Duration.ofDays(days)).toEpochMilli();
		List<Entry> recent = this.entries.findByTypeAndDateFrom("sgv", sinceMs);
		return compute(recent, bucketMinutes, offsetMinutes);
	}

	/**
	 * Pure-math AGP percentile computation. Given a flat list of SGV entries, bucket them
	 * by time-of-day in the caller's tz and return p5/p25/p50/p75/p95 per bucket. Empty
	 * buckets are skipped.
	 */
	public static List<AgpBucket> compute(List<Entry> entries, int bucketMinutes, int offsetMinutes) {
		if (bucketMinutes <= 0 || 1440 % bucketMinutes != 0) {
			throw new IllegalArgumentException("bucketMinutes must divide 1440");
		}
		int bucketCount = 1440 / bucketMinutes;
		List<List<Integer>> buckets = new ArrayList<>(bucketCount);
		for (int i = 0; i < bucketCount; i++) {
			buckets.add(new ArrayList<>());
		}

		for (Entry e : entries) {
			if (e.sgv() == null) {
				continue;
			}
			int minuteOfDay = (int) ((((e.dateMs() / 60_000L) + offsetMinutes) % 1440 + 1440) % 1440);
			int bucketIdx = minuteOfDay / bucketMinutes;
			buckets.get(bucketIdx).add(e.sgv());
		}

		List<AgpBucket> result = new ArrayList<>();
		for (int i = 0; i < bucketCount; i++) {
			List<Integer> bucket = buckets.get(i);
			if (bucket.isEmpty()) {
				continue;
			}
			Collections.sort(bucket);
			result.add(new AgpBucket(i * bucketMinutes, percentile(bucket, 0.05), percentile(bucket, 0.25),
					percentile(bucket, 0.50), percentile(bucket, 0.75), percentile(bucket, 0.95), bucket.size()));
		}
		return result;
	}

	/**
	 * Linear-interpolated percentile, matching Postgres {@code percentile_cont}. The
	 * input list must already be sorted ascending.
	 */
	static double percentile(List<Integer> sorted, double p) {
		int n = sorted.size();
		if (n == 0) {
			return 0.0;
		}
		if (n == 1) {
			return sorted.get(0);
		}
		double rank = p * (n - 1);
		int lower = (int) Math.floor(rank);
		int upper = (int) Math.ceil(rank);
		if (lower == upper) {
			return sorted.get(lower);
		}
		double weight = rank - lower;
		return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
	}

}
