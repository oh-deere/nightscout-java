package se.ohdeere.nightscout.service.bridge.impl;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibreLinkUpBridgeTests {

	@Test
	void parseLibreTimestamp() {
		Instant result = LibreLinkUpBridgeService.parseLibreTimestamp("1/15/2024 10:30:00 AM");
		assertThat(result).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
	}

	@Test
	void parseLibreTimestampPm() {
		Instant result = LibreLinkUpBridgeService.parseLibreTimestamp("12/25/2023 3:45:00 PM");
		assertThat(result).isEqualTo(Instant.parse("2023-12-25T15:45:00Z"));
	}

	@Test
	void mapTrendArrows() {
		assertThat(LibreLinkUpBridgeService.mapTrendArrow(1)).isEqualTo("SingleDown");
		assertThat(LibreLinkUpBridgeService.mapTrendArrow(3)).isEqualTo("Flat");
		assertThat(LibreLinkUpBridgeService.mapTrendArrow(5)).isEqualTo("SingleUp");
		assertThat(LibreLinkUpBridgeService.mapTrendArrow(null)).isNull();
	}

}
