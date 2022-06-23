package net.ttddyy.observation.tracing;

import java.net.URI;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Scope;
import net.ttddyy.dsproxy.ConnectionInfo;

/**
 * @author Tadaya Tsuyukubo
 */
public interface ConnectionAttributesManager {
	@Nullable
	ConnectionAttributes put(String connectionId, ConnectionAttributes attributes);

	@Nullable
	ConnectionAttributes get(String connectionId);

	@Nullable
	ConnectionAttributes remove(String connectionId);

	class ConnectionAttributes {

		ConnectionInfo connectionInfo;

		URI connectionUrl;

		Scope scope;
	}

}
