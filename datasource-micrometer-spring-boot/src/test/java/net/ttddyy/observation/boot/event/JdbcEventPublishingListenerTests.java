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

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link JdbcEventPublishingListener}.
 *
 * @author Tadaya Tsuyukubo
 */
class JdbcEventPublishingListenerTests {

	@Test
	void methodEvents() {
		AtomicReference<JdbcEvent> jdbcEventHolder = new AtomicReference<>();
		ApplicationContext context = createApplicationContextWithListener(jdbcEventHolder);
		JdbcEventPublishingListener listener = new JdbcEventPublishingListener(context);

		Connection source = mock(Connection.class);
		MethodExecutionContext methodContext = new MethodExecutionContext();
		methodContext.setProxy(source); // used as event source

		listener.beforeMethod(methodContext);

		assertThat(jdbcEventHolder).hasValueSatisfying((event) -> {
			assertThat(event).isInstanceOfSatisfying(JdbcMethodExecutionEvent.class, (ev) -> {
				assertThat(ev.isBefore()).isTrue();
				assertThat(ev.isAfter()).isFalse();
				assertThat(ev.getExecutionContext()).isSameAs(methodContext);
				assertThat(ev.getSource()).isSameAs(source);
			});
		});

		jdbcEventHolder.set(null); // reset

		listener.afterMethod(methodContext);

		assertThat(jdbcEventHolder).hasValueSatisfying((event) -> {
			assertThat(event).isInstanceOfSatisfying(JdbcMethodExecutionEvent.class, (ev) -> {
				assertThat(ev.isBefore()).isFalse();
				assertThat(ev.isAfter()).isTrue();
				assertThat(ev.getExecutionContext()).isSameAs(methodContext);
				assertThat(ev.getSource()).isSameAs(source);
			});
		});
	}

	@Test
	void queryEvents() {
		AtomicReference<JdbcEvent> jdbcEventHolder = new AtomicReference<>();
		ApplicationContext context = createApplicationContextWithListener(jdbcEventHolder);
		JdbcEventPublishingListener listener = new JdbcEventPublishingListener(context);

		Statement source = mock(Statement.class);
		ExecutionInfo executionInfo = new ExecutionInfo();
		executionInfo.setStatement(source); // used as event source
		listener.beforeQuery(executionInfo, Collections.emptyList());

		assertThat(jdbcEventHolder).hasValueSatisfying((event) -> {
			assertThat(event).isInstanceOfSatisfying(JdbcQueryExecutionEvent.class, (ev) -> {
				assertThat(ev.isBefore()).isTrue();
				assertThat(ev.isAfter()).isFalse();
				assertThat(ev.getExecInfo()).isSameAs(executionInfo);
				assertThat(ev.getSource()).isSameAs(source);
			});
		});

		jdbcEventHolder.set(null); // reset

		listener.afterQuery(executionInfo, Collections.emptyList());

		assertThat(jdbcEventHolder).hasValueSatisfying((event) -> {
			assertThat(event).isInstanceOfSatisfying(JdbcQueryExecutionEvent.class, (ev) -> {
				assertThat(ev.isBefore()).isFalse();
				assertThat(ev.isAfter()).isTrue();
				assertThat(ev.getExecInfo()).isSameAs(executionInfo);
				assertThat(ev.getSource()).isSameAs(source);
			});
		});
	}

	private ApplicationContext createApplicationContextWithListener(AtomicReference<JdbcEvent> eventHolder) {
		GenericApplicationContext context = new GenericApplicationContext();
		context.addApplicationListener((event) -> {
			if (event instanceof JdbcEvent) {
				eventHolder.set((JdbcEvent) event);
			}
		});
		context.refresh();
		return context;
	}

}
