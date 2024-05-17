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

import java.util.List;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;

/**
 * An event published when queries are executed.
 * <p>
 * The event is published right before and after executing the queries.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1
 */
public class JdbcQueryExecutionEvent extends JdbcEvent {

	private final boolean before;

	private final ExecutionInfo execInfo;

	private final List<QueryInfo> queryInfoList;

	public JdbcQueryExecutionEvent(boolean isBefore, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		super(execInfo.getStatement());
		this.before = isBefore;
		this.execInfo = execInfo;
		this.queryInfoList = queryInfoList;
	}

	/**
	 * {@code true} when the event is published right before executing queries.
	 * @return {@code true} if before execution
	 */
	public boolean isBefore() {
		return this.before;
	}

	/**
	 * {@code true} when the event is published right after executing queries.
	 * @return {@code true} if after execution
	 */
	public boolean isAfter() {
		return !this.before;
	}

	public ExecutionInfo getExecInfo() {
		return this.execInfo;
	}

	public List<QueryInfo> getQueryInfoList() {
		return this.queryInfoList;
	}

}
