package se.ohdeere.nightscout.api.v1.entries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Parses MongoDB-style query parameters (find[field][$op]=value) into SQL WHERE clauses.
 * Used across all v1 endpoints for consistent query behavior.
 */
class NightscoutQueryParser {

	private static final Map<String, String> FIELD_MAPPING = Map.ofEntries(Map.entry("date", "date_ms"),
			Map.entry("dateString", "date_string"), Map.entry("type", "type"), Map.entry("sgv", "sgv"),
			Map.entry("direction", "direction"), Map.entry("device", "device"), Map.entry("created_at", "created_at"),
			Map.entry("eventType", "event_type"), Map.entry("insulin", "insulin"), Map.entry("carbs", "carbs"),
			Map.entry("enteredBy", "entered_by"));

	record ParsedQuery(String whereClause, MapSqlParameterSource params, int limit, String sortField,
			boolean sortDesc) {
	}

	static ParsedQuery parse(Map<String, String[]> requestParams) {
		List<String> conditions = new ArrayList<>();
		MapSqlParameterSource params = new MapSqlParameterSource();
		int limit = 10;
		String sortField = "date_ms";
		boolean sortDesc = true;
		int paramIndex = 0;

		for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue()[0];

			if ("count".equals(key)) {
				limit = Math.min(Integer.parseInt(value), 1000);
				continue;
			}

			// Parse find[field][$op]=value or find[field]=value
			if (key.startsWith("find[")) {
				String fieldAndOp = key.substring(5);
				if (fieldAndOp.endsWith("]")) {
					fieldAndOp = fieldAndOp.substring(0, fieldAndOp.length() - 1);
				}

				String field;
				String op = "eq";
				if (fieldAndOp.contains("][$")) {
					String[] parts = fieldAndOp.split("\\]\\[\\$");
					field = parts[0];
					op = parts[1].replace("]", "");
				}
				else {
					field = fieldAndOp.replace("]", "");
				}

				String column = FIELD_MAPPING.getOrDefault(field, field);
				String paramName = "p" + paramIndex++;

				String sqlOp = switch (op) {
					case "gte" -> ">=";
					case "lte" -> "<=";
					case "gt" -> ">";
					case "lt" -> "<";
					case "ne" -> "!=";
					case "eq" -> "=";
					default -> "=";
				};

				conditions.add(column + " " + sqlOp + " :" + paramName);

				// Try to parse as number for numeric fields
				if (isNumericField(column)) {
					try {
						params.addValue(paramName, Long.parseLong(value));
					}
					catch (NumberFormatException ex) {
						params.addValue(paramName, value);
					}
				}
				else {
					params.addValue(paramName, value);
				}
			}
		}

		String where = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
		return new ParsedQuery(where, params, limit, sortField, sortDesc);
	}

	private static boolean isNumericField(String column) {
		return "date_ms".equals(column) || "sgv".equals(column) || "insulin".equals(column) || "carbs".equals(column);
	}

}
