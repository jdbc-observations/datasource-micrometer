package net.ttddyy.observation.tracing;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import net.ttddyy.observation.tracing.JdbcObservation.QueryHighCardinalityKeyNames;

/**
 * @author Tadaya Tsuyukubo
 */
public interface ResultSetKeyValuesProvider extends Observation.KeyValuesProvider<ResultSetContext> {
	@Override
	default boolean supportsContext(Context context) {
		return context instanceof ResultSetContext;
	}

	@Override
	default KeyValues getHighCardinalityKeyValues(ResultSetContext context) {
		return KeyValues.of(KeyValue.of(QueryHighCardinalityKeyNames.ROW_COUNT.name(), String.valueOf(context.getCount())));
	}
}
