package se.ohdeere.nightscout.docs;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Documents miscellaneous endpoints: profile, devicestatus, activity, properties, Loop
 * notifications, etc.
 */
class MiscDocsTest extends AbstractDocsTest {

	@Test
	void postProfile() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/profile.json")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{
					  "defaultProfile": "Default",
					  "store": {
					    "Default": {
					      "dia": 4,
					      "carbratio": [{"time": "00:00", "value": 10}],
					      "sens": [{"time": "00:00", "value": 50}],
					      "basal": [{"time": "00:00", "value": 0.8}],
					      "target_low": [{"time": "00:00", "value": 70}],
					      "target_high": [{"time": "00:00", "value": 180}],
					      "units": "mg/dl",
					      "timezone": "Europe/Stockholm"
					    }
					  }
					}
					"""));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-profile-post"));
	}

	@Test
	void getProfileCurrent() throws Exception {
		// Ensure there's at least one profile to return.
		this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/profile.json")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "defaultProfile": "Default", "store": { "Default": { "units": "mg/dl" } } }
					"""));
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/profile/current").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-profile-current"));
	}

	@Test
	void postDeviceStatus() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/devicestatus.json")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					[{
					  "device": "loop://iPhone",
					  "loop": {
					    "iob": {"iob": 1.5},
					    "predicted": {"values": [120, 118, 115]}
					  },
					  "pump": {"battery": {"percent": 80}, "reservoir": 100}
					}]
					"""));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-devicestatus-post"));
	}

	@Test
	void postActivity() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/activity")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "name": "morning walk", "duration": 30 }
					"""));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-activity-post"));
	}

	@Test
	void getProperties() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/properties").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-properties"));
	}

	@Test
	void postLoopNotification() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/notifications/loop")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{
					  "eventType": "Temporary Override",
					  "reason": "Exercise",
					  "reasonDisplay": "Exercise mode",
					  "duration": "60",
					  "notes": ""
					}
					"""));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v2-notifications-loop"));
	}

}
