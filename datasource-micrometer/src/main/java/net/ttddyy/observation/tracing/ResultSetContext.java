/*
 * Copyright 2022-2024 the original author or authors.
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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.observation.Observation;

/**
 * {@link Observation.Context Context} for {@link ResultSet} operations.
 *
 * @author Tadaya Tsuyukubo
 */
public final class ResultSetContext extends DataSourceBaseContext {

	private int count;

	private final List<ResultSetOperation> operations = new ArrayList<>();

	public void incrementCount() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<ResultSetOperation> getOperations() {
		return this.operations;
	}

	public boolean addOperation(ResultSetOperation operation) {
		return this.operations.add(operation);
	}

	public void clearOperations() {
		this.operations.clear();
	}

}
