package net.ttddyy.observation.tracing;

import java.net.URI;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;

/**
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionContext extends Observation.Context {

	private String dataSourceName;

	private URI url;

	public @Nullable URI getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable URI url) {
		this.url = url;
	}

	public String getDataSourceName() {
		return this.dataSourceName;
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}
}
