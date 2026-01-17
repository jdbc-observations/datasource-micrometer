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

import io.micrometer.core.instrument.MeterRegistry;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryAttributesManager;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryConnectionUrlParser;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryMeterObservationHandler;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryObservationConvention;
import net.ttddyy.observation.tracing.opentelemetry.SimpleDatabaseNamespaceRetriever;
import net.ttddyy.observation.tracing.opentelemetry.SimpleOpenTelemetryConnectionUrlParser;
import net.ttddyy.observation.tracing.opentelemetry.SqlServerDatabaseNamespaceRetriever;
import net.ttddyy.observation.tracing.opentelemetry.jsqlparser.JSqlParserQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.jsqlparser.JSqlParserSanitizingExpressionDeParser;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer Observation
 * instrumentation providing OpenTelemetry Semantic Conventions support.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.3.0
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@EnableConfigurationProperties(JdbcOpenTelemetryProperties.class)
@ConditionalOnClass({ JSqlParserQueryAnalyzer.class, JSqlParserSanitizingExpressionDeParser.class })
@ConditionalOnProperty(prefix = "jdbc.opentelemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSourceObservationOpenTelemetryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jdbc.opentelemetry.spans", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	OpenTelemetryQueryObservationConvention openTelemetryQueryObservationConvention(
			JdbcOpenTelemetryProperties properties, OpenTelemetryAttributesManager attributesManager) {
		OpenTelemetryQueryObservationConvention convention = new OpenTelemetryQueryObservationConvention(
				attributesManager);
		Map<String, String> overrides = properties.getAttributes().getOverrides();
		if (!overrides.isEmpty()) {
			convention.setAttributesOverrides(overrides);
		}
		return convention;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jdbc.opentelemetry.metrics", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	@ConditionalOnBean(MeterRegistry.class)
	OpenTelemetryMeterObservationHandler openTelemetryMeterObservationHandler(
			OpenTelemetryAttributesManager attributesManager, MeterRegistry meterRegistry) {
		return new OpenTelemetryMeterObservationHandler(attributesManager, meterRegistry);
	}

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetryAttributesManager openTelemetryAttributesManager(OpenTelemetryConnectionUrlParser connectionUrlParser,
			OpenTelemetryQueryAnalyzer queryAnalyzer) {
		return new OpenTelemetryAttributesManager(connectionUrlParser, queryAnalyzer);
	}

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetryConnectionUrlParser openTelemetryConnectionUrlParser() {
		SimpleDatabaseNamespaceRetriever simpleRetriever = new SimpleDatabaseNamespaceRetriever();
		SqlServerDatabaseNamespaceRetriever sqlServerRetriever = new SqlServerDatabaseNamespaceRetriever();
		OpenTelemetryConnectionUrlParser parser = new SimpleOpenTelemetryConnectionUrlParser(
				Map.of("mysql", simpleRetriever, "postgresql", simpleRetriever, "mariadb", simpleRetriever, "sqlserver",
						sqlServerRetriever));
		return new OpenTelemetryConnectionUrlParserCache(parser);
	}

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetryQueryAnalyzer openTelemetryQueryAnalyzer(JdbcOpenTelemetryProperties properties) {
		JdbcOpenTelemetryProperties.Analysis analysis = properties.getAnalysis();
		if (!analysis.isEnabled()) {
			return OpenTelemetryQueryAnalyzer.NOOP;
		}

		JSqlParserQueryAnalyzer analyzer = new JSqlParserQueryAnalyzer();
		if (!analysis.getSanitize().isEnabled()) {
			analyzer.setSanitizeEnabled(false);
		}
		if (!analysis.getSummary().isEnabled()) {
			analyzer.setSummaryEnabled(false);
		}
		if (!analysis.getCache().isEnabled()) {
			return analyzer;
		}

		int cacheSize = analysis.getCache().getMaxSize();
		return new OpenTelemetryQueryAnalyzerCache(analyzer, cacheSize);
	}

}
