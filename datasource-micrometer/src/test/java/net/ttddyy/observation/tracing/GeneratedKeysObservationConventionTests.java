package net.ttddyy.observation.tracing;

import io.micrometer.common.KeyValues;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedKeysObservationConventionTests {

	// gh-88
	@Test
	void getHighCardinalityKeyValuesWithNull() throws Exception {
		// public abstract boolean java.sql.ResultSet.next() throws java.sql.SQLException
		Method next = ResultSet.class.getMethod("next");
		Method getInt = ResultSet.class.getMethod("getInt", int.class);
		Method getString = ResultSet.class.getMethod("getString", int.class);

		ResultSetContext context = new ResultSetContext();
		context.addOperation(new ResultSetOperation(next, new Object[] {}, true, null));
		context.addOperation(new ResultSetOperation(getInt, new Object[] {}, 5, null));
		context.addOperation(new ResultSetOperation(getString, new Object[] {}, null, null)); // null

		GeneratedKeysObservationConvention convention = new GeneratedKeysObservationConvention() {
		};

		KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

		assertThat(keyValues)
			.filteredOn((kv) -> JdbcObservationDocumentation.GeneratedKeysHighCardinalityKeyNames.KEYS.asString()
				.equals(kv.getKey()))
			.first()
			.satisfies((kv) -> assertThat(kv.getValue()).isEqualTo("5,null"));
	}

}
