/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.observation.boot;

import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.proxy.JdbcProxyFactory;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.ProxyJdbcObject;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.proxy.jdk.CallableStatementInvocationHandler;
import net.ttddyy.dsproxy.proxy.jdk.ConnectionInvocationHandler;
import net.ttddyy.dsproxy.proxy.jdk.DataSourceInvocationHandler;
import net.ttddyy.dsproxy.proxy.jdk.PreparedStatementInvocationHandler;
import net.ttddyy.dsproxy.proxy.jdk.ResultSetInvocationHandler;
import net.ttddyy.dsproxy.proxy.jdk.StatementInvocationHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * {@link JdbcProxyFactory} implementation using spring's {@link ProxyFactory}.
 * <p>
 * Using Spring's proxy mechanism allows other Spring-based libraries to correctly
 * introspect and extract the underlying target object from the proxy.
 * <p>
 * This is particularly useful when Spring infrastructure components need to inspect the
 * concrete type of a proxied object. For example, Spring Cloud's refresh scope logic
 * checks whether a {@link DataSource} bean is a specific implementation such as HikariCP.
 * When the {@link DataSource} is wrapped by this proxy factory, Spring can still extract
 * the underlying target object and perform such checks correctly. (see issue#99)
 *
 * @author Tadaya Tsuyukubo
 * @since 1.4
 * @see ProxyFactory
 */
public class SpringJdbcProxyFactory implements JdbcProxyFactory {

	@Override
	public DataSource createDataSource(DataSource dataSource, ProxyConfig proxyConfig) {
		DataSourceInvocationHandler handler = new DataSourceInvocationHandler(dataSource, proxyConfig);
		return createProxy(dataSource, handler, DataSource.class);
	}

	@Override
	public Connection createConnection(Connection connection, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
		ConnectionInvocationHandler handler = new ConnectionInvocationHandler(connection, connectionInfo, proxyConfig);
		return createProxy(connection, handler, Connection.class);
	}

	@Override
	public Statement createStatement(Statement statement, ConnectionInfo connectionInfo, Connection proxyConnection,
			ProxyConfig proxyConfig) {
		StatementInvocationHandler handler = new StatementInvocationHandler(statement, connectionInfo, proxyConnection,
				proxyConfig);
		return createProxy(statement, handler, Statement.class);
	}

	@Override
	public PreparedStatement createPreparedStatement(PreparedStatement preparedStatement, String query,
			ConnectionInfo connectionInfo, Connection proxyConnection, ProxyConfig proxyConfig, boolean generateKey) {
		PreparedStatementInvocationHandler handler = new PreparedStatementInvocationHandler(preparedStatement, query,
				connectionInfo, proxyConnection, proxyConfig, generateKey);
		return createProxy(preparedStatement, handler, PreparedStatement.class);
	}

	@Override
	public CallableStatement createCallableStatement(CallableStatement callableStatement, String query,
			ConnectionInfo connectionInfo, Connection proxyConnection, ProxyConfig proxyConfig) {
		CallableStatementInvocationHandler handler = new CallableStatementInvocationHandler(callableStatement, query,
				connectionInfo, proxyConnection, proxyConfig);
		return createProxy(callableStatement, handler, CallableStatement.class);
	}

	@Override
	public ResultSet createResultSet(ResultSet resultSet, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
		ResultSetProxyLogicFactory factory = proxyConfig.getResultSetProxyLogicFactory();
		ResultSetInvocationHandler handler = new ResultSetInvocationHandler(factory, resultSet, connectionInfo,
				proxyConfig);
		return createProxy(resultSet, handler, ResultSet.class);
	}

	@Override
	public ResultSet createGeneratedKeys(ResultSet resultSet, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
		ResultSetProxyLogicFactory factory = proxyConfig.getGeneratedKeysProxyLogicFactory();
		ResultSetInvocationHandler handler = new ResultSetInvocationHandler(factory, resultSet, connectionInfo,
				proxyConfig);
		return createProxy(resultSet, handler, ResultSet.class);
	}

	private <T> T createProxy(Object target, InvocationHandler handler, Class<T> proxyClass) {
		MethodInterceptor interceptor = (invocation) -> {
			Object proxy;
			// JdkDynamicAopProxy(interfaced based proxy) creates
			// ReflectiveMethodInvocation
			if (invocation instanceof ProxyMethodInvocation proxyMethodInvocation) {
				proxy = proxyMethodInvocation.getProxy();
			}
			else {
				// should not happen, but just for a fallback
				proxy = invocation.getThis();
			}
			return handler.invoke(proxy, invocation.getMethod(), invocation.getArguments());
		};
		ProxyFactory proxyFactory = new ProxyFactory(ProxyJdbcObject.class, proxyClass);
		proxyFactory.addAdvice(interceptor);
		proxyFactory.setTarget(target);
		Object proxy = proxyFactory.getProxy();
		return proxyClass.cast(proxy);
	}

}
