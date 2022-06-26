package net.ttddyy.observation.tracing;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;

/**
 * @author Tadaya Tsuyukubo
 */
public class DataSourceBaseContext extends Observation.Context {

	private String dataSourceName;

	private String host;

	private int port;

	@Nullable
	public String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(@Nullable String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Nullable
	public String getHost() {
		return this.host;
	}

	public void setHost(@Nullable String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
