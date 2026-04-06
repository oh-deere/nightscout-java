package se.ohdeere.nightscout.remote.librelink;

import java.util.List;
import java.util.Optional;

public interface LibreLinkUpClient {

	Optional<AuthTicket> login(String email, String password, String apiHost);

	List<Connection> getConnections(AuthTicket ticket, String apiHost);

	GraphData getGraph(AuthTicket ticket, String apiHost, String patientId);

	record AuthTicket(String token, long expires, String userId, String apiHost) {

		public boolean isExpired() {
			return System.currentTimeMillis() / 1000 > this.expires;
		}

	}

	record Connection(String patientId, String firstName, String lastName) {
	}

	record GlucoseItem(String factoryTimestamp, String timestamp, int valueInMgPerDl, Integer trendArrow,
			boolean isHigh, boolean isLow) {
	}

	record GraphData(GlucoseItem currentMeasurement, List<GlucoseItem> graphData) {
	}

}
