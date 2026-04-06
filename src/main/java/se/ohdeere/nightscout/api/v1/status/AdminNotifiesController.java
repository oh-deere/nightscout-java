package se.ohdeere.nightscout.api.v1.status;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminNotifiesController {

	@GetMapping("/api/v1/adminnotifies")
	List<Object> adminNotifies() {
		return List.of();
	}

}
