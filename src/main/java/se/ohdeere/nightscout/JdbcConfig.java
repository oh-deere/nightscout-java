package se.ohdeere.nightscout;

import java.lang.reflect.Method;
import java.sql.JDBCType;
import java.util.List;

import se.ohdeere.nightscout.storage.JsonValue;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

@Configuration
class JdbcConfig extends AbstractJdbcConfiguration {

	@Override
	protected List<?> userConverters() {
		return List.of(new StringToJsonValueConverter(), new ObjectToJsonValueConverter(),
				new JsonValueWriteConverter());
	}

	@ReadingConverter
	static class StringToJsonValueConverter implements Converter<String, JsonValue> {

		@Override
		public JsonValue convert(String source) {
			return new JsonValue(source);
		}

	}

	/**
	 * Postgres jsonb columns come back as {@code org.postgresql.util.PGobject}, but the
	 * driver is on runtime classpath only. Use reflection to call {@code getValue()} so
	 * we don't need a compile-time dependency.
	 */
	@ReadingConverter
	static class ObjectToJsonValueConverter implements Converter<Object, JsonValue> {

		@Override
		public JsonValue convert(Object source) {
			if (source == null) {
				return null;
			}
			if (source instanceof JsonValue jv) {
				return jv;
			}
			if (source instanceof String s) {
				return new JsonValue(s);
			}
			// Try PGobject.getValue() reflectively
			try {
				Method getValue = source.getClass().getMethod("getValue");
				Object value = getValue.invoke(source);
				return new JsonValue(value != null ? value.toString() : null);
			}
			catch (NoSuchMethodException ex) {
				return new JsonValue(source.toString());
			}
			catch (ReflectiveOperationException ex) {
				throw new IllegalStateException("Failed to convert " + source.getClass() + " to JsonValue", ex);
			}
		}

	}

	@WritingConverter
	static class JsonValueWriteConverter implements Converter<JsonValue, JdbcValue> {

		@Override
		public JdbcValue convert(JsonValue source) {
			return JdbcValue.of(source.value(), JDBCType.OTHER);
		}

	}

}
