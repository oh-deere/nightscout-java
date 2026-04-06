package se.ohdeere.nightscout.storage;

/**
 * Wrapper type for jsonb columns in Spring Data JDBC. Prevents a global String converter
 * from interfering with regular TEXT columns.
 */
public record JsonValue(String value) {

	public static JsonValue of(String json) {
		return (json != null) ? new JsonValue(json) : null;
	}

	public static JsonValue empty() {
		return new JsonValue("{}");
	}

	@Override
	public String toString() {
		return this.value;
	}

}
