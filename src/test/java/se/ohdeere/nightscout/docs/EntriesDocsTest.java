package se.ohdeere.nightscout.docs;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class EntriesDocsTest extends AbstractDocsTest {

	@Test
	void postEntries() throws Exception {
		long now = System.currentTimeMillis();
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/entries.json")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					[{
					  "type": "sgv",
					  "date": %d,
					  "dateString": "2026-04-07T10:00:00.000Z",
					  "sysTime": "docs-%d",
					  "sgv": 142,
					  "direction": "Flat",
					  "device": "xDrip-DexcomG6",
					  "noise": 1
					}]
					""".formatted(now, now)));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-entries-post"));
	}

	@Test
	void getEntries() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/entries.json?count=10").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-entries-list"));
	}

	@Test
	void getCurrent() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/entries/current.json").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-entries-current"));
	}

	@Test
	void getSgv() throws Exception {
		ResultActions result = this.mockMvc.perform(
				MockMvcRequestBuilders.get("/api/v1/entries/sgv.json?count=10").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-entries-sgv"));
	}

	@Test
	void v3Entries() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v3/entries?limit=5").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v3-entries-list"));
	}

}
