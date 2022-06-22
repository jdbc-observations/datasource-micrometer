package net.ttddyy.observation.tracing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.common.lang.Nullable;

/**
 * @author Tadaya Tsuyukubo
 */
public class DefaultConnectionAttributesManager implements ConnectionAttributesManager {

	private final Map<String, ConnectionAttributes> connectionAttributesMap = new ConcurrentHashMap<>();

	@Override
	@Nullable
	public ConnectionAttributes put(String connectionId, ConnectionAttributes attributes) {
		return this.connectionAttributesMap.put(connectionId, attributes);
	}

	@Override
	public ConnectionAttributes get(String connectionId) {
		return this.connectionAttributesMap.get(connectionId);
	}


	@Override
	@Nullable
	public ConnectionAttributes remove(String connectionId) {
		return this.connectionAttributesMap.remove(connectionId);
	}

}
