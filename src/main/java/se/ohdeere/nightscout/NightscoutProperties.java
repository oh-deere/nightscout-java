package se.ohdeere.nightscout;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nightscout")
public record NightscoutProperties(String apiSecret, String units, int timeFormat, String theme, String language,
		String enable, Thresholds thresholds, int alarmTimeagoWarnMins, int alarmTimeagoUrgentMins) {

	public record Thresholds(int bgHigh, int bgTargetTop, int bgTargetBottom, int bgLow) {
	}

}
