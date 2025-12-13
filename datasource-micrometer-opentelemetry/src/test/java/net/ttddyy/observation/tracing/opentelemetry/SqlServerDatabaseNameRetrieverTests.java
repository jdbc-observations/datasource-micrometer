package net.ttddyy.observation.tracing.opentelemetry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SqlServerDatabaseNameRetriever}.
 */
class SqlServerDatabaseNameRetrieverTests {

	@ParameterizedTest
	@ValueSource(strings = {
	//@formatter:off
		"jdbc:sqlserver://192.168.1.1:1433;databaseName=myDB;user=admin;password=pass",
		"jdbc:sqlserver://localhost:1433;databaseName=myDB;integratedSecurity=true;",
		"jdbc:sqlserver://192.168.1.1\\myInstance;databaseName=myDB",
		"jdbc:sqlserver://192.168.1.1\\myInstance;databaseName=myDB",
		"jdbc:sqlserver://192.168.1.1:1433;databaseName=myDB;encrypt=true;trustServerCertificate=true;",
	//@formatter:on
	})
	void retrieve(String url) {
		SqlServerDatabaseNameRetriever retriever = new SqlServerDatabaseNameRetriever();
		String actual = retriever.retrieve(url);
		assertThat(actual).isEqualTo("myDB");
	}

}
