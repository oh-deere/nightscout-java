package se.ohdeere.nightscout.plugin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result from a plugin calculation, exposed via the status/properties API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PluginResult(String name, String label, String value, String info, String level,
		Map<String, Object> data) {

	public static PluginResult of(String name, String label, String value) {
		return new PluginResult(name, label, value, null, null, null);
	}

	public static PluginResult of(String name, String label, String value, String level) {
		return new PluginResult(name, label, value, null, level, null);
	}

	public static PluginResult withData(String name, String label, String value, Map<String, Object> data) {
		return new PluginResult(name, label, value, null, null, data);
	}

}
