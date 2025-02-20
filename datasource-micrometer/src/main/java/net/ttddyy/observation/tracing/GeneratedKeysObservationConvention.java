/*
 * Copyright 2024 the original author or authors.
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

import java.util.stream.Collectors;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationConvention;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation.GeneratedKeysHighCardinalityKeyNames;

/**
 * A {@link ObservationConvention} for generated keys.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1
 */
public interface GeneratedKeysObservationConvention extends ResultSetObservationConvention {

	@Override
	default KeyValues getHighCardinalityKeyValues(ResultSetContext context) {
		String keys = context.getOperations()
			.stream()
			.filter(ResultSetOperation::isDataRetrievalOperation)
			.map(ResultSetOperation::getResult)
			.map(Object::toString)
			.collect(Collectors.joining(","));
		return ResultSetObservationConvention.super.getHighCardinalityKeyValues(context)
			.and(KeyValue.of(GeneratedKeysHighCardinalityKeyNames.KEYS.asString(), keys));
	}

}
