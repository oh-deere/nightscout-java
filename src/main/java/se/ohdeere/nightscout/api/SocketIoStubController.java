package se.ohdeere.nightscout.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub Socket.IO endpoint. Returns a valid Engine.IO v4 handshake on first connect, then
 * long-polls (delays response ~25s) on subsequent requests to prevent the client from
 * hammering.
 */
@RestController
class SocketIoStubController {

	private static final String HANDSHAKE = "0{\"sid\":\"stub\",\"upgrades\":[],\"pingInterval\":25000,\"pingTimeout\":60000,\"maxPayload\":1000000}";

	@GetMapping({ "/socket.io", "/socket.io/" })
	Object poll(@RequestParam(value = "sid", required = false) String sid) {
		if (sid == null) {
			// Initial handshake — respond immediately
			return ResponseEntity.ok().header("Content-Type", "text/plain;charset=UTF-8").body(HANDSHAKE);
		}
		// Subsequent polls — long-poll: delay 25s then return a noop/ping
		return CompletableFuture.supplyAsync(() -> {
			try {
				TimeUnit.SECONDS.sleep(25);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return ResponseEntity.ok().header("Content-Type", "text/plain;charset=UTF-8").body("2");
		});
	}

	@PostMapping({ "/socket.io", "/socket.io/" })
	ResponseEntity<String> post() {
		return ResponseEntity.ok("ok");
	}

}
