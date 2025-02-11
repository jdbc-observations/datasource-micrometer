/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing;

import java.util.HashSet;
import java.util.Set;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation.QueryHighCardinalityKeyNames;

import static net.ttddyy.observation.tracing.JdbcObservationDocumentation.*;

/**
 * A {@link ObservationConvention} for query.
 *
 * @author Tadaya Tsuyukubo
 */
public interface QueryObservationConvention extends BaseObservationConvention<QueryContext> {

	@Override
	default boolean supportsContext(Context context) {
		return context instanceof QueryContext;
	}

	@Override
	default String getName() {
		return "jdbc.query";
	}

	@Override
	default KeyValues getLowCardinalityKeyValues(QueryContext context) {
		Set<KeyValue> keyValues = new HashSet<>();
		getDatasourceName(context).ifPresent(keyValues::add);
		return KeyValues.of(keyValues);
    }

	@Override
	default KeyValues getHighCardinalityKeyValues(QueryContext context) {
		Set<KeyValue> keyValues = new HashSet<>();
		for (int i = 0; i < context.getQueries().size(); i++) {
			String key = context.getQueries().get(i);
			String queryKey = String.format(QueryHighCardinalityKeyNames.QUERY.asString(), i);
			keyValues.add(KeyValue.of(queryKey, key));
		}
		// params could be empty when "includeParameterValues=false" in the listener.
		for (int i = 0; i < context.getParams().size(); i++) {
			String params = context.getParams().get(i);
			String key = String.format(QueryHighCardinalityKeyNames.QUERY_PARAMETERS.asString(), i);
			keyValues.add(KeyValue.of(key, params));
		}
		String affectedRowCount = context.getAffectedRowCount();
		if (affectedRowCount != null) {
			keyValues.add(KeyValue.of(QueryHighCardinalityKeyNames.ROW_AFFECTED, affectedRowCount));
		}
		return KeyValues.of(keyValues);
	}

}
