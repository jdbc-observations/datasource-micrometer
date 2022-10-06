/*
 * Copyright 2022 the original author or authors.
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

package net.ttddyy.observation.tracing;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * {@link ObservationDocumentation} for JDBC operations.
 *
 * @author Tadaya Tsuyukubo
 */
public enum JdbcObservation implements ObservationDocumentation {

	/**
	 * Span created when a JDBC connection takes place.
	 */
	CONNECTION {
		@Override
		public String getName() {
			return "jdbc.connection";
		}

		@Override
		public String getContextualName() {
			return "connection";
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return ConnectionKeyNames.values();
		}

		@Override
		public String getPrefix() {
			return "jdbc";
		}

		@Override
		public Event[] getEvents() {
			return JdbcEvents.values();
		}

	},

	/**
	 * Span created when executing a query.
	 */
	QUERY {
		@Override
		public String getName() {
			return "jdbc.query";
		}

		@Override
		public String getContextualName() {
			return "query";
		}

		// TODO: add connection-id, url, thread to low cardinality keys to match with
		// r2dbc.

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return QueryHighCardinalityKeyNames.values();
		}

		@Override
		public String getPrefix() {
			return "jdbc";
		}
	},

	/**
	 * Span created when working with JDBC result set.
	 */
	RESULT_SET {
		@Override
		public String getName() {
			return "jdbc.result-set";
		}

		@Override
		public String getContextualName() {
			return "result-set";
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return ResultSetHighCardinalityKeyNames.values();
		}

		@Override
		public String getPrefix() {
			return "jdbc";
		}
	};

	enum QueryHighCardinalityKeyNames implements KeyName {

		/**
		 * Name of the JDBC query.
		 */
		QUERY {
			@Override
			public String asString() {
				return "jdbc.query[%s]";
			}
		},

		/**
		 * JDBC query parameter values.
		 */
		QUERY_PARAMETERS {
			@Override
			public String asString() {
				return "jdbc.params[%s]";
			}
		},

		/**
		 * Result of "executeUpdate()", "executeLargeUpdate()", "executeBatch()", or
		 * "executeLargeBatch()" on "Statement". For batch operations, the value is
		 * represented as array.
		 */
		ROW_AFFECTED {
			@Override
			public String asString() {
				return "jdbc.row-affected";
			}
		}

	}

	enum ResultSetHighCardinalityKeyNames implements KeyName {

		/**
		 * Number of SQL rows.
		 */
		ROW_COUNT {
			@Override
			public String asString() {
				return "jdbc.row-count";
			}
		}

	}

	enum ConnectionKeyNames implements KeyName {

		/**
		 * Name of the JDBC datasource driver. (HikariCP only)
		 */
		DATASOURCE_DRIVER {
			@Override
			public String asString() {
				return "jdbc.datasource.driver";
			}
		},

		/**
		 * Name of the JDBC datasource pool. (HikariCP only)
		 */
		DATASOURCE_POOL {
			@Override
			public String asString() {
				return "jdbc.datasource.pool";
			}
		},

	}

	enum JdbcEvents implements Event {

		/**
		 * When the connection is acquired. This event is recorded right after successful
		 * "getConnection()" call.
		 */
		CONNECTION_ACQUIRED {
			@Override
			public String getName() {
				return "acquired";
			}

			@Override
			public String getContextualName() {
				return "acquired";
			}
		},

		/**
		 * When the connection is committed.
		 */
		CONNECTION_COMMIT {
			@Override
			public String getName() {
				return "commit";
			}

			@Override
			public String getContextualName() {
				return "commit";
			}
		},

		/**
		 * When the connection is rolled back.
		 */
		CONNECTION_ROLLBACK {
			@Override
			public String getName() {
				return "rollback";
			}

			@Override
			public String getContextualName() {
				return "rollback";
			}
		},

	}

}
