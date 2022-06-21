package net.ttddyy.observation.tracing;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.DocumentedObservation;

/**
 * @author Tadaya Tsuyukubo
 */
public enum JdbcObservation implements DocumentedObservation {
	QUERY {
		@Override
		public String getName() {
			return "query";
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

}
