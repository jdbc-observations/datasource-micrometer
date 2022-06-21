package net.ttddyy.observation.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;

/**
 * @author Tadaya Tsuyukubo
 */
public interface QueryKeyValuesProvider extends Observation.KeyValuesProvider<QueryContext> {
	@Override
	default boolean supportsContext(Context context) {
		return context instanceof QueryContext;
	}
}
