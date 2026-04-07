package se.ohdeere.nightscout.api.v1.status;

import java.util.LinkedHashMap;
import java.util.Map;

import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.plugin.ages.ConsumableAgePlugin;
import se.ohdeere.nightscout.plugin.alarm.AlarmEngine;
import se.ohdeere.nightscout.plugin.ar2.Ar2Plugin;
import se.ohdeere.nightscout.plugin.bgnow.BgNowPlugin;
import se.ohdeere.nightscout.plugin.cob.CobPlugin;
import se.ohdeere.nightscout.plugin.iob.IobPlugin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PropertiesController {

	private final BgNowPlugin bgNowPlugin;

	private final IobPlugin iobPlugin;

	private final CobPlugin cobPlugin;

	private final Ar2Plugin ar2Plugin;

	private final ConsumableAgePlugin agePlugin;

	private final AlarmEngine alarmEngine;

	PropertiesController(BgNowPlugin bgNowPlugin, IobPlugin iobPlugin, CobPlugin cobPlugin, Ar2Plugin ar2Plugin,
			ConsumableAgePlugin agePlugin, AlarmEngine alarmEngine) {
		this.bgNowPlugin = bgNowPlugin;
		this.iobPlugin = iobPlugin;
		this.cobPlugin = cobPlugin;
		this.ar2Plugin = ar2Plugin;
		this.agePlugin = agePlugin;
		this.alarmEngine = alarmEngine;
	}

	@GetMapping("/api/v1/properties")
	Map<String, Object> properties() {
		AuthHelper.requirePermission("entries", "read");
		Map<String, Object> props = new LinkedHashMap<>();

		this.bgNowPlugin.calculate().ifPresent(r -> props.put("bgnow", r));
		this.iobPlugin.calculate().ifPresent(r -> props.put("iob", r));
		this.cobPlugin.calculate().ifPresent(r -> props.put("cob", r));
		this.ar2Plugin.calculate().ifPresent(r -> props.put("ar2", r));
		this.agePlugin.cage().ifPresent(r -> props.put("cage", r));
		this.agePlugin.sage().ifPresent(r -> props.put("sage", r));
		this.agePlugin.iage().ifPresent(r -> props.put("iage", r));
		this.agePlugin.bage().ifPresent(r -> props.put("bage", r));

		AlarmEngine.Alarm alarm = this.alarmEngine.currentAlarm();
		if (alarm != null) {
			props.put("alarm", alarm);
		}

		return props;
	}

	@PostMapping("/api/v1/notifications/ack")
	Map<String, String> acknowledgeAlarm(@RequestBody(required = false) Map<String, Object> body) {
		return doAcknowledge(body != null ? body : Map.of());
	}

	/**
	 * Upstream Nightscout exposes notification ack as a GET with query parameters. AAPS
	 * uses this form, while xDrip+ uses POST.
	 */
	@GetMapping("/api/v1/notifications/ack")
	Map<String, String> acknowledgeAlarmGet(
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer level,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String group,
			@org.springframework.web.bind.annotation.RequestParam(required = false) Integer silenceMinutes) {
		return doAcknowledge(Map.of("level", level != null ? level : 0, "group", group != null ? group : "",
				"silenceMinutes", silenceMinutes != null ? silenceMinutes : 30));
	}

	private Map<String, String> doAcknowledge(Map<String, Object> body) {
		AuthHelper.requirePermission("notifications", "create");
		int silenceMinutes = (body.get("silenceMinutes") instanceof Number n) ? n.intValue() : 30;
		AlarmEngine.Alarm current = this.alarmEngine.currentAlarm();
		if (current != null) {
			this.alarmEngine.snooze(current.type(), silenceMinutes);
		}
		return Map.of("result", "ok");
	}

}
