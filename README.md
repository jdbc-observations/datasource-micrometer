# datasource-micrometer

## Modules

`datasource-micrometer`  
Micrometer observability instrumentation for JDBC DataSource.

`datasource-micrometer-spring-boot`  
Spring Boot 3.x AutoConfiguration for micrometer observability.


## Java versions

The produced jars have following runtime JDK versions supported:

- `datasource-micrometer`: Java 8+ to match with micrometer 1.x java baseline.
- `datasource-micrometer-spring-boot`: Java 17+ to match with Spring Boot 3.x java baseline.


## Development

To build the entire project, JDK-17 is required.

### Building document

```shell
./mvnw install -Pdocs -pl docs
```
