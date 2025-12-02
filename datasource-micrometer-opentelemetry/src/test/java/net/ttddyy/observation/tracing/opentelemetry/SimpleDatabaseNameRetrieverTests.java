package net.ttddyy.observation.tracing.opentelemetry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleDatabaseNameRetrieverTests {

	@ParameterizedTest
	@ValueSource(strings = {
	//@formatter:off
		"jdbc:mysql://localhost:3306/myDB",
		"jdbc:mysql://localhost:3306/myDB?useSSL=false",
		"jdbc:mysql://192.168.1.100:3307/myDB",
		"jdbc:mysql://localhost:3306/myDB?useSSL=false&serverTimezone=UTC",
		"jdbc:mysql://localhost:3306/myDB?user=admin&password=pass",
		"jdbc:postgresql://hostname:5432/myDB"
	//@formatter:on
	})
	void retrieve(String url) {
		SimpleDatabaseNameRetriever retriever = new SimpleDatabaseNameRetriever();
		String actual = retriever.retrieve(url);
		assertThat(actual).isEqualTo("myDB");
	}

}
