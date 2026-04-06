package se.ohdeere.nightscout.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub Socket.IO endpoint that returns a valid Engine.IO handshake response. Prevents the
 * Nightscout UI from spamming 404 errors while still allowing it to function via API
 * polling.
 */
@RestController
class SocketIoStubController {

	private static final String HANDSHAKE = "0{\"sid\":\"stub\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":60000,\"maxPayload\":1000000}";

	@GetMapping({ "/socket.io", "/socket.io/" })
	ResponseEntity<String> poll() {
		return ResponseEntity.ok().header("Content-Type", "text/plain;charset=UTF-8").body(HANDSHAKE);
	}

	@PostMapping({ "/socket.io", "/socket.io/" })
	ResponseEntity<String> post() {
		return ResponseEntity.ok("ok");
	}

}
