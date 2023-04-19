/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.observation.tracing.JdbcObservationDocumentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JDBC instrumentation.
 *
 * @author Arthur Gavlyukovskiy
 * @author Tadaya Tsuyukubo
 */
@ConfigurationProperties("jdbc")
public class JdbcProperties {

	/**
	 * Which types of tracing we would like to include.
	 */
	private Set<TraceType> includes = Set.of(TraceType.CONNECTION, TraceType.QUERY, TraceType.FETCH);

	/**
	 * List of DataSource bean names that will not be decorated.
	 */
	private Set<String> excludedDataSourceBeanNames = new HashSet<>();

	private DataSourceProxy datasourceProxy = new DataSourceProxy();

	public Set<TraceType> getIncludes() {
		return this.includes;
	}

	public void setIncludes(Set<TraceType> includes) {
		this.includes = includes;
	}

	public Set<String> getExcludedDataSourceBeanNames() {
		return this.excludedDataSourceBeanNames;
	}

	public void setExcludedDataSourceBeanNames(Set<String> excludedDataSourceBeanNames) {
		this.excludedDataSourceBeanNames = excludedDataSourceBeanNames;
	}

	public DataSourceProxy getDatasourceProxy() {
		return this.datasourceProxy;
	}

	public void setDatasourceProxy(DataSourceProxy datasourceProxy) {
		this.datasourceProxy = datasourceProxy;
	}

	public static class DataSourceProxy {

		/**
		 * Logging to use for logging queries.
		 */
		private Logging logging = Logging.SLF4J;

		/**
		 * Query configuration.
		 */
		private Query query = new Query();

		/**
		 * Slow query configuration.
		 */
		private SlowQuery slowQuery = new SlowQuery();

		/**
		 * Use multiline output for logging query.
		 *
		 * @see ProxyDataSourceBuilder#multiline()
		 */
		private boolean multiline = true;

		/**
		 * Whether to tag actual query parameter values.
		 */
		private boolean includeParameterValues;

		/**
		 * Use json output for logging query.
		 *
		 * @see ProxyDataSourceBuilder#asJson()
		 */
		private boolean jsonFormat = false;

		public Logging getLogging() {
			return this.logging;
		}

		public void setLogging(Logging logging) {
			this.logging = logging;
		}

		public Query getQuery() {
			return this.query;
		}

		public void setQuery(Query query) {
			this.query = query;
		}

		public SlowQuery getSlowQuery() {
			return this.slowQuery;
		}

		public void setSlowQuery(SlowQuery slowQuery) {
			this.slowQuery = slowQuery;
		}

		public boolean isMultiline() {
			return this.multiline;
		}

		public void setMultiline(boolean multiline) {
			this.multiline = multiline;
		}

		public boolean isJsonFormat() {
			return this.jsonFormat;
		}

		public void setJsonFormat(boolean jsonFormat) {
			this.jsonFormat = jsonFormat;
		}

		public boolean isIncludeParameterValues() {
			return this.includeParameterValues;
		}

		public void setIncludeParameterValues(boolean includeParameterValues) {
			this.includeParameterValues = includeParameterValues;
		}

	}

	/**
	 * Properties to configure query logging listener.
	 */
	public static class Query {

		/**
		 * Enable logging all queries to the log.
		 */
		private boolean enableLogging = false;

		/**
		 * Name of query logger.
		 */
		private String loggerName;

		/**
		 * Severity of query logger.
		 */
		private String logLevel = "DEBUG";

		public boolean isEnableLogging() {
			return this.enableLogging;
		}

		public void setEnableLogging(boolean enableLogging) {
			this.enableLogging = enableLogging;
		}

		public String getLoggerName() {
			return this.loggerName;
		}

		public void setLoggerName(String loggerName) {
			this.loggerName = loggerName;
		}

		public String getLogLevel() {
			return this.logLevel;
		}

		public void setLogLevel(String logLevel) {
			this.logLevel = logLevel;
		}

	}

	/**
	 * Properties to configure slow query logging listener.
	 */
	public static class SlowQuery {

		/**
		 * Enable logging slow queries to the log.
		 */
		private boolean enableLogging = false;

		/**
		 * Name of slow query logger.
		 */
		private String loggerName;

		/**
		 * Severity of slow query logger.
		 */
		private String logLevel = "WARN";

		/**
		 * Number of seconds to consider query as slow.
		 */
		private long threshold = 300;

		public boolean isEnableLogging() {
			return enableLogging;
		}

		public void setEnableLogging(boolean enableLogging) {
			this.enableLogging = enableLogging;
		}

		public String getLoggerName() {
			return loggerName;
		}

		public void setLoggerName(String loggerName) {
			this.loggerName = loggerName;
		}

		public String getLogLevel() {
			return logLevel;
		}

		public void setLogLevel(String logLevel) {
			this.logLevel = logLevel;
		}

		public long getThreshold() {
			return threshold;
		}

		public void setThreshold(long threshold) {
			this.threshold = threshold;
		}

	}

	/**
	 * Query logging listener is the most used listener that logs executing query with
	 * actual parameters to. You can pick one of the following proxy logging mechanisms.
	 */
	public enum Logging {

		/**
		 * Log using System.out.
		 */
		SYSOUT,

		/**
		 * Log using SLF4J.
		 */
		SLF4J,

		/**
		 * Log using Commons.
		 */
		COMMONS,

		/**
		 * Log using Java Util Logging.
		 */
		JUL,

		/**
		 * Log using Log4j.
		 */
		LOG4J

	}

	public enum TraceType {

		/**
		 * Related to JDBC connections.
		 */
		CONNECTION(JdbcObservationDocumentation.CONNECTION),

		/**
		 * Related to query executions.
		 */
		QUERY(JdbcObservationDocumentation.QUERY),

		/**
		 * Related to ResultSets.
		 */
		FETCH(JdbcObservationDocumentation.RESULT_SET);

		final JdbcObservationDocumentation supportedDocumentation;

		TraceType(JdbcObservationDocumentation supportedDocumentation) {
			this.supportedDocumentation = supportedDocumentation;
		}

	}

}
