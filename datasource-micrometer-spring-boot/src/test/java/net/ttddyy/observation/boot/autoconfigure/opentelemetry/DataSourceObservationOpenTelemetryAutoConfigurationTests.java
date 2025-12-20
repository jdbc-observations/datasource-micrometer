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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryMeterObservationHandler;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryAnalyzer;
import net.ttddyy.observation.tracing.opentelemetry.OpenTelemetryQueryObservationConvention;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceObservationOpenTelemetryAutoConfiguration}.
 */
class DataSourceObservationOpenTelemetryAutoConfigurationTests {

	ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceObservationOpenTelemetryAutoConfiguration.class));

	@Test
	void enabled() {
		// default enabled
		this.runner.run((context) -> assertThat(context)
			.hasSingleBean(DataSourceObservationOpenTelemetryAutoConfiguration.class));

		// disabled by property
		this.runner.withPropertyValues("jdbc.opentelemetry.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(DataSourceObservationOpenTelemetryAutoConfiguration.class));

		// disabled by classpath
		this.runner.withClassLoader(new FilteredClassLoader("net.ttddyy.observation.tracing.opentelemetry"))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(DataSourceObservationOpenTelemetryAutoConfiguration.class));
	}

	@Test
	void spansConditions() {
		// default enabled
		this.runner.run((context) -> assertThat(context).hasSingleBean(OpenTelemetryQueryObservationConvention.class));

		// disabled by property
		this.runner.withPropertyValues("jdbc.opentelemetry.spans.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OpenTelemetryQueryObservationConvention.class));
	}

	@Test
	void metricsConditions() {
		// default enabled
		this.runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> assertThat(context).hasSingleBean(OpenTelemetryMeterObservationHandler.class));

		// disabled when no MeterRegistry bean
		this.runner.run((context) -> assertThat(context).doesNotHaveBean(OpenTelemetryMeterObservationHandler.class));

		// disabled by property
		this.runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
			.withPropertyValues("jdbc.opentelemetry.metrics.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OpenTelemetryMeterObservationHandler.class));
	}

	@Test
	void openTelemetryQueryAnalyzerCache() {
		// default
		this.runner.run((context) -> assertThat(context).getBean("openTelemetryQueryAnalyzer")
			.isInstanceOf(OpenTelemetryQueryAnalyzerCache.class));

		// cache disabled
		this.runner.withPropertyValues("jdbc.opentelemetry.analysis.cache.enabled=false")
			.run((context) -> assertThat(context).getBean("openTelemetryQueryAnalyzer")
				.isInstanceOf(OpenTelemetryQueryAnalyzer.class));
	}

	@Test
	void openTelemetryQueryAnalyzerDisabled() {
		// default
		this.runner.run((context) -> assertThat(context).getBean("openTelemetryQueryAnalyzer")
			.isInstanceOf(OpenTelemetryQueryAnalyzerCache.class));

		// cache disabled
		this.runner.withPropertyValues("jdbc.opentelemetry.analysis.enabled=false")
			.run((context) -> assertThat(context).getBean("openTelemetryQueryAnalyzer")
				.isSameAs(OpenTelemetryQueryAnalyzer.NOOP));
	}

}
