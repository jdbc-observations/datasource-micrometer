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

package net.ttddyy.observation.boot.event;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;

/**
 * An event published when JDBC proxy methods are invoked.
 * <p>
 * The event is published right before and after executing the proxy methods.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1
 */
public class JdbcMethodExecutionEvent extends JdbcEvent {

	private final boolean before;

	private final MethodExecutionContext executionContext;

	public JdbcMethodExecutionEvent(boolean before, MethodExecutionContext executionContext) {
		super(executionContext.getProxy());
		this.before = before;
		this.executionContext = executionContext;
	}

	/**
	 * {@code true} when the event is published right before performing the method.
	 * @return {@code true} if before execution
	 */
	public boolean isBefore() {
		return this.before;
	}

	/**
	 * {@code true} when the event is published right after performing the method.
	 * @return {@code true} if after execution
	 */
	public boolean isAfter() {
		return !this.before;
	}

	public MethodExecutionContext getExecutionContext() {
		return this.executionContext;
	}

}
