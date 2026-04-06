package se.ohdeere.nightscout;

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
		return List.of(new JsonValueReadConverter(), new JsonValueWriteConverter());
	}

	@ReadingConverter
	static class JsonValueReadConverter implements Converter<String, JsonValue> {

		@Override
		public JsonValue convert(String source) {
			return new JsonValue(source);
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
