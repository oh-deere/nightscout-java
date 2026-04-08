package se.ohdeere.nightscout.api.v1.agp;

import java.util.List;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.service.agp.AgpService;
import se.ohdeere.nightscout.service.agp.AgpService.AgpBucket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ambulatory Glucose Profile endpoint. Returns one row per non-empty time-of-day bucket
 * with percentile bands for the trailing window. The frontend overlays these as faded
 * bands behind the live SGV trace so the user can immediately compare "right now" to
 * "typical at this time of day".
 */
@RestController
class AgpController {

	private final AgpService agp;

	AgpController(AgpService agp) {
		this.agp = agp;
	}

	@GetMapping("/api/v1/agp")
	List<AgpBucket> getAgp(@RequestParam(defaultValue = "14") int days,
			@RequestParam(defaultValue = "15") int bucketMinutes, @RequestParam(defaultValue = "0") int offsetMinutes) {
		AuthHelper.requirePermission("entries", "read");
		return this.agp.getAgp(days, bucketMinutes, offsetMinutes);
	}

}
