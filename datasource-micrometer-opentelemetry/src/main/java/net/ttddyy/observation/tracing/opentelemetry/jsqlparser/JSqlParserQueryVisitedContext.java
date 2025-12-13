/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.tracing.opentelemetry.jsqlparser;

import io.micrometer.common.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Context for {@link JSqlParserQueryVisitor}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class JSqlParserQueryVisitedContext {

	private final List<VisitedEntry> entries = new ArrayList<>();

	@Nullable
	private String mainTableName;

	public List<VisitedEntry> getEntries() {
		return this.entries;
	}

	public void addOperation(String operationName) {
		this.entries.add(VisitedEntry.operation(operationName));
	}

	public void addCollection(String collectionName) {
		this.entries.add(VisitedEntry.collection(collectionName));
	}

	public void dedupeLastTwoEntries() {
		if (this.entries.size() < 2) {
			return;
		}
		VisitedEntry last = this.entries.remove(this.entries.size() - 1);
		VisitedEntry secondLast = this.entries.get(this.entries.size() - 1);
		if (!last.equals(secondLast)) {
			this.entries.add(last);
		}
	}

	/**
	 * Main table name for prepared statement. For callable statement, this becomes the
	 * name of procedure.
	 * @return name
	 */
	@Nullable
	public String getMainTableName() {
		return this.mainTableName;
	}

	public void setMainTableName(@Nullable String mainTableName) {
		this.mainTableName = mainTableName;
	}

}
