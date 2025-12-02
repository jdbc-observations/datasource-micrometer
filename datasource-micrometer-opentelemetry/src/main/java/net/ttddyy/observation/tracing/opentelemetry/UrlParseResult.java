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

package net.ttddyy.observation.tracing.opentelemetry;

import io.micrometer.common.lang.Nullable;

/**
 * @since 1.3.0
 */
public class UrlParseResult {

	private String systemName = "other_sql";

	@Nullable
	private String databaseName;

	public String getSystemName() {
		return this.systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	@Nullable
	public String getDatabaseName() {
		return this.databaseName;
	}

	public void setDatabaseName(@Nullable String databaseName) {
		this.databaseName = databaseName;
	}

}
