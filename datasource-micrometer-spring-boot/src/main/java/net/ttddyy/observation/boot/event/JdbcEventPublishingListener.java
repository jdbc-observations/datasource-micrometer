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
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

import org.springframework.context.ApplicationEventPublisher;

/**
 * A datasource-proxy listener that publishes spring events for jdbc interactions.
 *
 * @author Tadaya Tsuyukubo
 * @since 1.1
 */
public class JdbcEventPublishingListener implements QueryExecutionListener, MethodExecutionListener {

	private final ApplicationEventPublisher publisher;

	public JdbcEventPublishingListener(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void beforeMethod(MethodExecutionContext executionContext) {
		this.publisher.publishEvent(new JdbcMethodExecutionEvent(true, executionContext));
	}

	@Override
	public void afterMethod(MethodExecutionContext executionContext) {
		this.publisher.publishEvent(new JdbcMethodExecutionEvent(false, executionContext));
	}

	@Override
	public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		this.publisher.publishEvent(new JdbcQueryExecutionEvent(true, execInfo, queryInfoList));
	}

	@Override
	public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		this.publisher.publishEvent(new JdbcQueryExecutionEvent(false, execInfo, queryInfoList));
	}

}
