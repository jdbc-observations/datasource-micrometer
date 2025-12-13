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

package net.ttddyy.observation.boot.autoconfigure.opentelemetry;

import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryAttributesManager;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryConnectionUrlParser;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryObservationConvention;
import net.ttddyy.observation.tracing.opentelemetry.SimpleDatabaseNameRetriever;
import net.ttddyy.observation.tracing.opentelemetry.SimpleOpenTelemetryConnectionUrlParser;
import net.ttddyy.observation.tracing.opentelemetry.SqlServerDatabaseNameRetriever;
import net.ttddyy.observation.tracing.opentelemetry.jsqlparser.JSqlParserQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.jsqlparser.JSqlParserSanitizingExpressionDeParser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@AutoConfiguration()
// @EnableConfigurationProperties(JdbcProperties.class)
 @ConditionalOnClass({ JSqlParserQueryAnalyzer.class, JSqlParserSanitizingExpressionDeParser.class })
// @ConditionalOnProperty(prefix = "jdbc.datasource-proxy", name = "enabled", havingValue
// = "true", matchIfMissing = true)
public class DataSourceObservationOpenTelemetryAutoConfiguration {

	@Bean
	OpenTelemetryQueryObservationConvention openTelemetryQueryObservationConvention(
			OpenTelemetryAttributesManager attributesManager) {
		return new OpenTelemetryQueryObservationConvention(attributesManager);
	}

	@Bean
	OpenTelemetryAttributesManager openTelemetryAttributesManager(OpenTelemetryConnectionUrlParser connectionUrlParser,
			OpenTelemetryQueryAnalyzer queryAnalyzer) {
		return new OpenTelemetryAttributesManager(connectionUrlParser, queryAnalyzer);
	}

	@Bean
	OpenTelemetryConnectionUrlParser openTelemetryConnectionUrlParser() {
		SimpleDatabaseNameRetriever simpleRetriever = new SimpleDatabaseNameRetriever();
		SqlServerDatabaseNameRetriever sqlServerRetriever = new SqlServerDatabaseNameRetriever();
		OpenTelemetryConnectionUrlParser parser = new SimpleOpenTelemetryConnectionUrlParser(
				Map.of("mysql", simpleRetriever, "postgresql", simpleRetriever, "mariadb", simpleRetriever, "sqlserver",
						sqlServerRetriever));
		return new OpenTelemetryConnectionUrlParserCache(parser);
	}

	@Bean
	OpenTelemetryQueryAnalyzer openTelemetryQueryAnalyzer() {
		// TODO: property for cache size, parserConfigurer, executorService
		JSqlParserQueryAnalyzer analayzer = new JSqlParserQueryAnalyzer();
		return new OpenTelemetryQueryAnalyzerCache(analayzer, 1000);
	}

}
