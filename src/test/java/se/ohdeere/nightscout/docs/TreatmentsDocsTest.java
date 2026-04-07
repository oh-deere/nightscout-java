package se.ohdeere.nightscout.docs;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class TreatmentsDocsTest extends AbstractDocsTest {

	@Test
	void postTreatments() throws Exception {
		ResultActions result = this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/treatments.json")
			.header("api-secret", API_SECRET_HASH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					[{
					  "eventType": "Meal Bolus",
					  "created_at": "2026-04-07T10:00:00.000Z",
					  "carbs": 30,
					  "insulin": 2.5,
					  "enteredBy": "loop://iPhone",
					  "syncIdentifier": "docs-treatment-1",
					  "insulinType": "novolog"
					}]
					"""));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-treatments-post"));
	}

	@Test
	void getTreatments() throws Exception {
		ResultActions result = this.mockMvc.perform(
				MockMvcRequestBuilders.get("/api/v1/treatments.json?count=10").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v1-treatments-list"));
	}

	@Test
	void v3Treatments() throws Exception {
		ResultActions result = this.mockMvc
			.perform(MockMvcRequestBuilders.get("/api/v3/treatments?limit=10").header("api-secret", API_SECRET_HASH));
		result.andExpect(MockMvcResultMatchers.status().isOk());
		result.andDo(MockMvcRestDocumentation.document("v3-treatments-list"));
	}

}
