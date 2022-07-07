/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.Log4jLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import net.ttddyy.observation.boot.autoconfigure.JdbcProperties.TraceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * Configurer for {@link ProxyDataSourceBuilder} based on the application context.
 *
 * @author Arthur Gavlyukovskiy
 * @author Tadaya Tsuyukubo
 * @see ProxyDataSourceBuilder
 */
public class DataSourceProxyBuilderConfigurer {

	private static final Log log = LogFactory.getLog(DataSourceProxyBuilderConfigurer.class);

	private final List<QueryExecutionListener> listeners;

	private final List<MethodExecutionListener> methodExecutionListeners;

	private final ParameterTransformer parameterTransformer;

	private final QueryTransformer queryTransformer;

	private final ResultSetProxyLogicFactory resultSetProxyLogicFactory;

	private final DataSourceProxyConnectionIdManagerProvider dataSourceProxyConnectionIdManagerProvider;

	private final JdbcProperties jdbcProperties;

	public DataSourceProxyBuilderConfigurer(JdbcProperties jdbcProperties,
			@Nullable List<QueryExecutionListener> listeners,
			@Nullable List<MethodExecutionListener> methodExecutionListeners,
			@Nullable ParameterTransformer parameterTransformer, @Nullable QueryTransformer queryTransformer,
			@Nullable ResultSetProxyLogicFactory resultSetProxyLogicFactory,
			@Nullable DataSourceProxyConnectionIdManagerProvider dataSourceProxyConnectionIdManagerProvider) {
		this.jdbcProperties = jdbcProperties;
		this.listeners = listeners;
		this.methodExecutionListeners = methodExecutionListeners;
		this.parameterTransformer = parameterTransformer;
		this.queryTransformer = queryTransformer;
		this.resultSetProxyLogicFactory = resultSetProxyLogicFactory;
		this.dataSourceProxyConnectionIdManagerProvider = dataSourceProxyConnectionIdManagerProvider;
	}

	public ProxyDataSourceBuilder configure(ProxyDataSourceBuilder proxyDataSourceBuilder) {
		JdbcProperties.DataSourceProxy datasourceProxy = this.jdbcProperties.getDatasourceProxy();
		switch (datasourceProxy.getLogging()) {
			case SLF4J -> {
				if (datasourceProxy.getQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logQueryBySlf4j(toSlf4JLogLevel(datasourceProxy.getQuery().getLogLevel()),
							datasourceProxy.getQuery().getLoggerName());
				}
				if (datasourceProxy.getSlowQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logSlowQueryBySlf4j(datasourceProxy.getSlowQuery().getThreshold(),
							TimeUnit.SECONDS, toSlf4JLogLevel(datasourceProxy.getSlowQuery().getLogLevel()),
							datasourceProxy.getSlowQuery().getLoggerName());
				}
			}
			case LOG4J -> {
				if (datasourceProxy.getQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logQueryByLog4j(toLog4jLevel(datasourceProxy.getQuery().getLogLevel()),
							datasourceProxy.getQuery().getLoggerName());
				}
				if (datasourceProxy.getSlowQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logSlowQueryByLog4j(datasourceProxy.getSlowQuery().getThreshold(),
							TimeUnit.SECONDS, toLog4jLevel(datasourceProxy.getSlowQuery().getLogLevel()),
							datasourceProxy.getSlowQuery().getLoggerName());
				}
			}
			case JUL -> {
				if (datasourceProxy.getQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logQueryByJUL(toJULLogLevel(datasourceProxy.getQuery().getLogLevel()),
							datasourceProxy.getQuery().getLoggerName());
				}
				if (datasourceProxy.getSlowQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logSlowQueryByJUL(datasourceProxy.getSlowQuery().getThreshold(),
							TimeUnit.SECONDS, toJULLogLevel(datasourceProxy.getSlowQuery().getLogLevel()),
							datasourceProxy.getSlowQuery().getLoggerName());
				}
			}
			case COMMONS -> {
				if (datasourceProxy.getQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logQueryByCommons(
							toCommonsLogLevel(datasourceProxy.getQuery().getLogLevel()),
							datasourceProxy.getQuery().getLoggerName());
				}
				if (datasourceProxy.getSlowQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logSlowQueryByCommons(datasourceProxy.getSlowQuery().getThreshold(),
							TimeUnit.SECONDS, toCommonsLogLevel(datasourceProxy.getSlowQuery().getLogLevel()),
							datasourceProxy.getSlowQuery().getLoggerName());
				}
			}
			case SYSOUT -> {
				if (datasourceProxy.getQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logQueryToSysOut();
				}
				if (datasourceProxy.getSlowQuery().isEnableLogging()) {
					proxyDataSourceBuilder.logSlowQueryToSysOut(datasourceProxy.getSlowQuery().getThreshold(),
							TimeUnit.SECONDS);
				}
			}
		}
		if (datasourceProxy.isMultiline() && datasourceProxy.isJsonFormat()) {
			log.warn(
					"Found opposite multiline and json format, multiline will be used (may depend on library version)");
		}
		if (datasourceProxy.isMultiline()) {
			proxyDataSourceBuilder.multiline();
		}
		if (datasourceProxy.isJsonFormat()) {
			proxyDataSourceBuilder.asJson();
		}

		if (this.jdbcProperties.getIncludes().contains(TraceType.FETCH)) {
			ResultSetProxyLogicFactory factory = this.resultSetProxyLogicFactory == null
					? ResultSetProxyLogicFactory.DEFAULT : this.resultSetProxyLogicFactory;
			proxyDataSourceBuilder.proxyResultSet(factory);
		}
		ifAvailable(this.listeners, l -> l.forEach(proxyDataSourceBuilder::listener));
		ifAvailable(this.methodExecutionListeners, m -> m.forEach(proxyDataSourceBuilder::methodListener));
		ifAvailable(this.parameterTransformer, proxyDataSourceBuilder::parameterTransformer);
		ifAvailable(this.queryTransformer, proxyDataSourceBuilder::queryTransformer);
		ifAvailable(this.dataSourceProxyConnectionIdManagerProvider,
				d -> proxyDataSourceBuilder.connectionIdManager(d.get()));
		return proxyDataSourceBuilder;
	}

	private <T> void ifAvailable(@Nullable T o, Consumer<T> consumer) {
		if (o != null) {
			consumer.accept(o);
		}
	}

	private SLF4JLogLevel toSlf4JLogLevel(String logLevel) {
		for (SLF4JLogLevel slf4JLogLevel : SLF4JLogLevel.values()) {
			if (slf4JLogLevel.name().equalsIgnoreCase(logLevel)) {
				return slf4JLogLevel;
			}
		}
		throw new IllegalArgumentException("Unresolved log level " + logLevel + " for slf4j logger, known levels: "
				+ Arrays.toString(SLF4JLogLevel.values()));
	}

	private Level toJULLogLevel(String logLevel) {
		try {
			return Level.parse(logLevel);
		}
		catch (IllegalArgumentException e) {
			if (logLevel.equalsIgnoreCase("DEBUG")) {
				return Level.FINE;
			}
			if (logLevel.equalsIgnoreCase("WARN")) {
				return Level.WARNING;
			}
			throw new IllegalArgumentException("Unresolved log level " + logLevel + " for java.util.logging", e);
		}
	}

	private CommonsLogLevel toCommonsLogLevel(String logLevel) {
		for (CommonsLogLevel commonsLogLevel : CommonsLogLevel.values()) {
			if (commonsLogLevel.name().equalsIgnoreCase(logLevel)) {
				return commonsLogLevel;
			}
		}
		throw new IllegalArgumentException("Unresolved log level " + logLevel
				+ " for apache commons logger, known levels " + Arrays.toString(CommonsLogLevel.values()));
	}

	private Log4jLogLevel toLog4jLevel(String logLevel) {
		for (Log4jLogLevel log4jLogLevel : Log4jLogLevel.values()) {
			if (log4jLogLevel.name().equalsIgnoreCase(logLevel)) {
				return log4jLogLevel;
			}
		}
		throw new IllegalArgumentException("Unresolved log level " + logLevel
				+ " for apache log4j logger, known levels " + Arrays.toString(Log4jLogLevel.values()));
	}

}
