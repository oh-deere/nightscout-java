package se.ohdeere.nightscout.service.agp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import se.ohdeere.nightscout.storage.entries.Entry;
import se.ohdeere.nightscout.storage.entries.EntryRepository;

import org.springframework.scheduling.annotation.Scheduled;
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

	private record CacheKey(int days, int bucketMinutes, int offsetMinutes) {
	}

	private record CacheEntry(List<AgpBucket> value, Instant computedAt) {
	}

	private static final long CACHE_TTL_MS = 5L * 60 * 1000;

	private final EntryRepository entries;

	private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

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
		CacheKey key = new CacheKey(days, bucketMinutes, offsetMinutes);
		CacheEntry hit = this.cache.get(key);
		Instant now = Instant.now();
		if (hit != null && Duration.between(hit.computedAt(), now).toMillis() < CACHE_TTL_MS) {
			return hit.value();
		}
		long sinceMs = now.minus(Duration.ofDays(days)).toEpochMilli();
		List<Entry> recent = this.entries.findByTypeAndDateFrom("sgv", sinceMs);
		List<AgpBucket> result = compute(recent, bucketMinutes, offsetMinutes);
		this.cache.put(key, new CacheEntry(result, now));
		return result;
	}

	/**
	 * Find the AGP bucket that contains a given timestamp + offset, in the existing
	 * computed result. Returns empty when the bucket has no rows (sparse data) so callers
	 * can fall back gracefully.
	 */
	public static Optional<AgpBucket> bucketAt(List<AgpBucket> buckets, long dateMs, int bucketMinutes,
			int offsetMinutes) {
		if (buckets == null || buckets.isEmpty() || bucketMinutes <= 0) {
			return Optional.empty();
		}
		int minuteOfDay = (int) ((((dateMs / 60_000L) + offsetMinutes) % 1440 + 1440) % 1440);
		int bucketMinute = (minuteOfDay / bucketMinutes) * bucketMinutes;
		for (AgpBucket b : buckets) {
			if (b.bucketMinute() == bucketMinute) {
				return Optional.of(b);
			}
		}
		return Optional.empty();
	}

	/**
	 * Estimate where {@code sgv} falls in the bucket's distribution as a 0–100 percentile
	 * rank. Uses the same five anchor points the bucket exposes (p5, p25, p50, p75, p95)
	 * and linear-interpolates between them; readings below p5 clamp to 0, above p95 clamp
	 * to 100.
	 */
	public static double percentileRank(AgpBucket bucket, int sgv) {
		if (bucket == null) {
			return Double.NaN;
		}
		double[] xs = { bucket.p5(), bucket.p25(), bucket.p50(), bucket.p75(), bucket.p95() };
		double[] ys = { 5, 25, 50, 75, 95 };
		if (sgv <= xs[0]) {
			return 0;
		}
		if (sgv >= xs[4]) {
			return 100;
		}
		for (int i = 0; i < xs.length - 1; i++) {
			if (sgv >= xs[i] && sgv <= xs[i + 1]) {
				double span = xs[i + 1] - xs[i];
				if (span == 0) {
					return ys[i];
				}
				double t = (sgv - xs[i]) / span;
				return ys[i] + t * (ys[i + 1] - ys[i]);
			}
		}
		return Double.NaN;
	}

	@Scheduled(fixedRate = 3600000, initialDelay = 60000)
	void evictStaleCache() {
		Instant cutoff = Instant.now().minusMillis(CACHE_TTL_MS * 2);
		this.cache.entrySet().removeIf(e -> e.getValue().computedAt().isBefore(cutoff));
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
