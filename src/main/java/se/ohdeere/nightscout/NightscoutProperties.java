package se.ohdeere.nightscout;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap-only configuration. Everything that an admin can change at runtime lives in
 * the {@code runtime_settings} table and is read through
 * {@link se.ohdeere.nightscout.service.admin.EffectiveSettings}.
 *
 * <p>
 * Only three fields remain here:
 * <ul>
 * <li>{@code apiSecret} — bootstrap admin credential, must exist before any authenticated
 * request can land</li>
 * <li>{@code enable} / {@code showPlugins} — plugin gating that affects which beans the
 * application exposes; needs to be known at startup, not at request time</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "nightscout")
public record NightscoutProperties(String apiSecret, String enable, String showPlugins) {
}
