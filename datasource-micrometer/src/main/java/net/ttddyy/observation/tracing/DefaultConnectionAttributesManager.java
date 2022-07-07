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
