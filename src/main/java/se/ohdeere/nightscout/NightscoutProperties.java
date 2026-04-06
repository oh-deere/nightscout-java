package se.ohdeere.nightscout;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nightscout")
public record NightscoutProperties(String apiSecret, String units, int timeFormat, String theme, String language,
		String enable, String showPlugins, String customTitle, String alarmTypes, String authDefaultRoles,
		boolean nightMode, boolean devicestatusAdvanced, int bolusRenderOver, Thresholds thresholds,
		int alarmTimeagoWarnMins, int alarmTimeagoUrgentMins, Sage sage) {

	public record Thresholds(int bgHigh, int bgTargetTop, int bgTargetBottom, int bgLow) {
	}

	public record Sage(int info, int warn, int urgent) {
	}

	/**
	 * Convert mg/dL to display units (mg/dl or mmol/l).
	 */
	public double toDisplayUnits(int mgdl) {
		if ("mmol/l".equalsIgnoreCase(this.units) || "mmol".equalsIgnoreCase(this.units)) {
			return Math.round(mgdl / 18.0 * 10.0) / 10.0;
		}
		return mgdl;
	}

}
