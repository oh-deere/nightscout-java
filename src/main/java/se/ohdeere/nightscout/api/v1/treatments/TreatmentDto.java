package se.ohdeere.nightscout.api.v1.treatments;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.treatments.Treatment;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TreatmentDto(@JsonProperty("_id") String id, String eventType, @JsonProperty("created_at") String createdAtStr,
		String enteredBy, String notes, Double insulin, Double carbs, Double glucose, String glucoseType,
		Double duration, int utcOffset) {

	static TreatmentDto from(Treatment t) {
		return new TreatmentDto(t.id() != null ? t.id().toString() : null, t.eventType(), t.createdAtStr(),
				t.enteredBy(), t.notes(), t.insulin(), t.carbs(), t.glucose(), t.glucoseType(), t.duration(),
				t.utcOffset());
	}

	Treatment toTreatment() {
		Instant parsed = (this.createdAtStr != null) ? Instant.parse(this.createdAtStr) : Instant.now();
		return new Treatment(null, this.eventType, this.createdAtStr, parsed, this.enteredBy, this.notes, this.insulin,
				this.carbs, this.glucose, this.glucoseType, this.duration, this.utcOffset, JsonValue.empty());
	}

}
