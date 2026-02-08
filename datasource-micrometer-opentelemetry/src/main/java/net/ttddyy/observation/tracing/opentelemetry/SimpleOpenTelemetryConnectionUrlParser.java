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

import java.util.Map;

/**
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 **/
public class SimpleOpenTelemetryConnectionUrlParser implements OpenTelemetryConnectionUrlParser {

	private final Map<String, DatabaseNamespaceRetriever> namespaceRetrievers;

	public SimpleOpenTelemetryConnectionUrlParser(Map<String, DatabaseNamespaceRetriever> namespaceRetrievers) {
		this.namespaceRetrievers = namespaceRetrievers;
	}

	public UrlParseResult parse(String url) {
		String systemName = geDatabaseType(url);
		String namespace = getNamespace(systemName, url);

		UrlParseResult result = new UrlParseResult();
		result.setSystemName(systemName);
		result.setNamespace(namespace);
		return result;
	}

	// db.system.name
	private String geDatabaseType(String url) {
		if (url.startsWith("jdbc:postgresql:")) {
			return "postgresql";
		}
		else if (url.startsWith("jdbc:mysql:")) {
			return "mysql";
		}
		else if (url.startsWith("jdbc:mariadb:")) {
			return "mariadb";
		}
		else if (url.startsWith("jdbc:sqlserver:")) {
			return "microsoft.sql_server";
		}
		return "other_sql";
	}

	@Nullable
	private String getNamespace(String systemName, String url) {
		DatabaseNamespaceRetriever dbNameRetriever = this.namespaceRetrievers.get(systemName);
		if (dbNameRetriever == null) {
			return null;
		}
		return dbNameRetriever.retrieve(url);
	}

}
