package net.ttddyy.observation.tracing;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.DocumentedObservation;

/**
 * @author Tadaya Tsuyukubo
 */
public enum JdbcObservation implements DocumentedObservation {

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
			return new KeyName[] { ConnectionKeyNames.DATASOURCE_DRIVER };
		}

		@Override
		public String getPrefix() {
			return "jdbc";
		}
	},


	QUERY {
		@Override
		public String getName() {
			return "jdbc.query";
		}

		@Override
		public String getContextualName() {
			return "query";
		}

		// TODO: add connection-id, url, thread to low cardinality keys to match with r2dbc.

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
			return new KeyName[] { QueryHighCardinalityKeyNames.ROW_COUNT };
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
			public String getKeyName() {
				return "jdbc.query[%s]";
			}
		},

		/**
		 * Number of SQL rows.
		 */
		ROW_COUNT {
			@Override
			public String getKeyName() {
				return "jdbc.row-count";
			}
		}
	}

	enum ConnectionKeyNames implements KeyName {

		/**
		 * Name of the JDBC datasource driver.
		 */
		DATASOURCE_DRIVER {
			@Override
			public String getKeyName() {
				return "jdbc.datasource.driver";
			}
		},

		/**
		 * Name of the JDBC datasource pool.
		 */
		DATASOURCE_POOL {
			@Override
			public String getKeyName() {
				return "jdbc.datasource.pool";
			}
		},

	}

}
