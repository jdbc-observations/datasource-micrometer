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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieve database namespace from JDBC url for SQL Server.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
public class SqlServerDatabaseNamespaceRetriever implements DatabaseNamespaceRetriever {

	private static final Pattern PATTERN = Pattern.compile("(?<=databaseName=)[^;]+", Pattern.CASE_INSENSITIVE);

	@Nullable
	@Override
	public String retrieve(String url) {
		Matcher matcher = PATTERN.matcher(url);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

}
