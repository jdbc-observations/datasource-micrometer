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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

	private static final Set<String> NON_DATA_RETRIEVAL_METHODS = new HashSet<>();
	static {
		// start with "get" but not data retrieval
		NON_DATA_RETRIEVAL_METHODS
			.addAll(Arrays.asList("getConcurrency", "getCursorName", "getMetaData", "getFetchDirection", "getFetchSize",
					"getHoldability", "getRow", "getStatement", "getType", "getWarnings"));
	}

	public static boolean isDataRetrievalOperation(ResultSetOperation op) {
		String methodName = op.getMethod().getName();
		return methodName.startsWith("get") && !NON_DATA_RETRIEVAL_METHODS.contains(methodName);
	}

	public Method getMethod() {
		return this.method;
	}

	public Object getResult() {
		return this.result;
	}

}
