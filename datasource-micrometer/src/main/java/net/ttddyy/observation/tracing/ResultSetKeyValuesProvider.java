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
		return KeyValues
				.of(KeyValue.of(QueryHighCardinalityKeyNames.ROW_COUNT.name(), String.valueOf(context.getCount())));
	}

}
