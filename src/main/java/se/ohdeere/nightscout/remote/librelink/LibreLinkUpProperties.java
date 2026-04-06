package se.ohdeere.nightscout.remote.librelink;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "librelink")
public record LibreLinkUpProperties(boolean enabled, String username, String password, String region,
		long pollIntervalMs, String connectionId, int retryAttempts, long retryIntervalMs) {

	public String apiHost() {
		return switch (this.region.toUpperCase()) {
			case "US" -> "api-us.libreview.io";
			case "EU" -> "api-eu.libreview.io";
			case "EU2" -> "api-eu2.libreview.io";
			case "DE" -> "api-de.libreview.io";
			case "FR" -> "api-fr.libreview.io";
			case "CA" -> "api-ca.libreview.io";
			case "AU" -> "api-au.libreview.io";
			case "AE" -> "api-ae.libreview.io";
			case "AP" -> "api-ap.libreview.io";
			case "JP" -> "api-jp.libreview.io";
			case "LA" -> "api-la.libreview.io";
			default -> "api-eu.libreview.io";
		};
	}

}
