/*
 * Copyright 2024 the original author or authors.
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

import java.lang.reflect.Method;
import java.sql.ResultSet;

/**
 * Represent an operation(method call) performed on the proxy {@link ResultSet}.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1
 */
public class ResultSetOperation {

	private final Method method;

	private final Object result;

	public ResultSetOperation(Method method, Object result) {
		this.method = method;
		this.result = result;
	}

	public Method getMethod() {
		return this.method;
	}

	public Object getResult() {
		return this.result;
	}

}
