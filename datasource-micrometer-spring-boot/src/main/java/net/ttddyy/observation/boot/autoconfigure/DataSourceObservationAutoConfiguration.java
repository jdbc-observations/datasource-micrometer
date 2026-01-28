/*
 * Copyright 2022-2025 the original author or authors.
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

package net.ttddyy.observation.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.GlobalConnectionIdManager;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import net.ttddyy.observation.boot.autoconfigure.JdbcProperties.TraceType;
import net.ttddyy.observation.boot.event.JdbcEventPublishingListener;
import net.ttddyy.observation.tracing.ConnectionObservationConvention;
import net.ttddyy.observation.tracing.ConnectionTracingObservationHandler;
import net.ttddyy.observation.tracing.DataSourceObservationListener;
import net.ttddyy.observation.tracing.GeneratedKeysObservationConvention;
import net.ttddyy.observation.tracing.HikariJdbcObservationFilter;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation;
import net.ttddyy.observation.tracing.QueryObservationConvention;
import net.ttddyy.observation.tracing.QueryTracingObservationHandler;
import net.ttddyy.observation.tracing.ResultSetObservationConvention;
import net.ttddyy.observation.tracing.ResultSetTracingObservationHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.boot.micrometer.tracing.autoconfigure.ConditionalOnEnabledTracingExport;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer Observation
 * instrumentation for {@link DataSource}.
 *
 * @author Tadaya Tsuyukubo
 */
@AutoConfiguration(after = { ObservationAutoConfiguration.class, MicrometerTracingAutoConfiguration.class,
		DataSourceAutoConfiguration.class })
@EnableConfigurationProperties(JdbcProperties.class)
@ConditionalOnClass({ DataSource.class, ObservationRegistry.class })
@ConditionalOnProperty(prefix = "jdbc.datasource-proxy", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSourceObservationAutoConfiguration {

	public static final int OBSERVATION_LISTENER_ORDER = 1000;

	public static final int EVENT_PUBLISHING_LISTENER_ORDER = OBSERVATION_LISTENER_ORDER + 100;

	@Bean
	@Order(OBSERVATION_LISTENER_ORDER)
	public DataSourceObservationListener dataSourceObservationListener(ObjectProvider<ObservationRegistry> registry,
			JdbcProperties jdbcProperties, ObjectProvider<ObservationConvention<?>> observationConventions) {
		Set<JdbcObservationDocumentation> supportedDocumentations = jdbcProperties.getIncludes()
			.stream()
			.map((include) -> include.supportedDocumentation)
			.collect(Collectors.toSet());
		// to avoid circular reference due to MeterBinder creation at MeterRegistry,
		// use supplier to lazily reference observation registry.
		DataSourceObservationListener listener = new DataSourceObservationListener(registry::getObject);
		listener.setIncludeParameterValues(jdbcProperties.getDatasourceProxy().isIncludeParameterValues());
		listener.setSupportedTypes(supportedDocumentations);
		listener.setIncludeResultSetOperations(jdbcProperties.getResultsetOperations().isEnabled());
		observationConventions.orderedStream().forEach((convention) -> {
			if (convention instanceof ConnectionObservationConvention) {
				listener.setConnectionObservationConvention((ConnectionObservationConvention) convention);
			}
			else if (convention instanceof QueryObservationConvention) {
				listener.setQueryObservationConvention((QueryObservationConvention) convention);
			}
			else if (convention instanceof GeneratedKeysObservationConvention) {
				listener.setGeneratedKeysObservationConvention((GeneratedKeysObservationConvention) convention);
			}
			else if (convention instanceof ResultSetObservationConvention) {
				listener.setResultSetObservationConvention((ResultSetObservationConvention) convention);
			}
		});
		return listener;
	}

	@Bean
	@ConditionalOnMissingBean
	public DataSourceProxyConnectionIdManagerProvider observationConnectionIdManagerProvider() {
		return GlobalConnectionIdManager::new;
	}

	@Bean
	@ConditionalOnMissingBean
	public DataSourceNameResolver observationDataSourceNameResolver() {
		return new DefaultDataSourceNameResolver();
	}

	@Bean
	public static DataSourceObservationBeanPostProcessor dataSourceObservationBeanPostProcessor(
			ObjectProvider<JdbcProperties> jdbcProperties,
			ObjectProvider<DataSourceNameResolver> dataSourceNameResolvers,
			ObjectProvider<QueryExecutionListener> listeners,
			ObjectProvider<MethodExecutionListener> methodExecutionListeners,
			ObjectProvider<ParameterTransformer> parameterTransformer,
			ObjectProvider<QueryTransformer> queryTransformer,
			ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactory,
			ObjectProvider<ResultSetProxyLogicFactory> generatedKeysProxyLogicFactory,
			ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProvider,
			ObjectProvider<ProxyDataSourceBuilderCustomizer> proxyDataSourceBuilderCustomizers) {
		return new DataSourceObservationBeanPostProcessor(jdbcProperties, dataSourceNameResolvers, listeners,
				methodExecutionListeners, parameterTransformer, queryTransformer, resultSetProxyLogicFactory,
				generatedKeysProxyLogicFactory, dataSourceProxyConnectionIdManagerProvider,
				proxyDataSourceBuilderCustomizers);
	}

	@Bean
	@Order(EVENT_PUBLISHING_LISTENER_ORDER)
	@ConditionalOnProperty(prefix = "jdbc.event", name = "enabled", havingValue = "true")
	JdbcEventPublishingListener jdbcEventPublishingListener(ApplicationEventPublisher publisher) {
		return new JdbcEventPublishingListener(publisher);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.zaxxer.hikari.HikariDataSource")
	static class Hikari {

		@Bean
		public HikariJdbcObservationFilter hikariJdbcObservationFilter() {
			return new HikariJdbcObservationFilter();
		}

		@Bean
		public ObservationRegistryCustomizer<ObservationRegistry> hikariJdbcObservationFilterObservationRegistryCustomizer(
				HikariJdbcObservationFilter hikariJdbcObservationFilter) {
			return registry -> registry.observationConfig().observationFilter(hikariJdbcObservationFilter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledTracingExport
	static class DataSourceTracing {

		private static final int ORDER = MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER
				- 1000;

		@Bean
		@ConditionalOnTraceType(TraceType.CONNECTION)
		@Order(ORDER)
		public ConnectionTracingObservationHandler connectionTracingObservationHandler(Tracer tracer) {
			return new ConnectionTracingObservationHandler(tracer);
		}

		@Bean
		@ConditionalOnTraceType(TraceType.QUERY)
		@Order(ORDER)
		public QueryTracingObservationHandler queryTracingObservationHandler(Tracer tracer) {
			return new QueryTracingObservationHandler(tracer);
		}

		@Bean
		@ConditionalOnTraceType(TraceType.FETCH)
		@Order(ORDER)
		public ResultSetTracingObservationHandler resultSetTracingObservationHandler(Tracer tracer) {
			return new ResultSetTracingObservationHandler(tracer);
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OnTraceTypeCondition.class)
	@interface ConditionalOnTraceType {

		TraceType value();

	}

	static class OnTraceTypeCondition extends SpringBootCondition {

		private static final String INCLUDES_PROP_KEY = "jdbc.includes";

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnTraceType.class.getName());
			Assert.notNull(attributes, "attributes must not be null");
			TraceType requiredTraceType = (TraceType) attributes.get("value");
			Environment environment = context.getEnvironment();
			Set<TraceType> traceTypes = Set.of();

			StringBuilder details = new StringBuilder();
			details.append("(");
			details.append(INCLUDES_PROP_KEY);
			details.append("=");
			if (environment.containsProperty(INCLUDES_PROP_KEY)) {
				try {
					traceTypes = Binder.get(environment)
						.bindOrCreate(INCLUDES_PROP_KEY, Bindable.setOf(TraceType.class));
					details.append(environment.getProperty(INCLUDES_PROP_KEY));
				}
				catch (BindException ex) {
					details.append("<Bind Failed>");
				}
			}
			else {
				// use default values by config properties
				traceTypes = new JdbcProperties().getIncludes();
				details.append(traceTypes);
				details.append("(default properties)");
			}
			details.append(")");

			ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnTraceType.class, details);
			if (traceTypes.contains(requiredTraceType)) {
				return ConditionOutcome.match(message.found("TraceType").items(requiredTraceType));
			}
			return ConditionOutcome.noMatch(message.didNotFind("TraceType").items(requiredTraceType));
		}

	}

}
