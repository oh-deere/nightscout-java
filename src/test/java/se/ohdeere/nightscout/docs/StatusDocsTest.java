package se.ohdeere.nightscout.docs;

import org.junit.jupiter.api.Test;

import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class StatusDocsTest extends AbstractDocsTest {

	@Test
	void apiVersions() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/versions"));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("api-versions"));
	}

	@Test
	void status() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/status.json"));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-status"));
	}

	@Test
	void verifyAuth() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/verifyauth").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-verifyauth"));
	}

	@Test
	void experimentsTest() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v1/experiments/test").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-experiments-test"));
	}

	@Test
	void v3Version() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v3/version"));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v3-version"));
	}

	@Test
	void v3Status() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v3/status"));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v3-status"));
	}

	@Test
	void v3LastModified() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v3/lastModified").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v3-lastmodified"));
	}

	@Test
	void v2AuthorizationRequest() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v2/authorization/request/" + API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v2-authorization-request"));
	}

}
