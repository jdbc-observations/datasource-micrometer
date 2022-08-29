# Datasource Micrometer [![Maven Central](https://maven-badges.herokuapp.com/maven-central/jdbc-observations/datasource-micrometer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/jdbc-observations/datasource-micrometer)

The Datasource Micrometer provides a [Micrometer Tracing][micrometer-tracing] instrumentation for JDBC operations.

[micrometer-tracing]: https://micrometer.io/docs/tracing

## Modules

**datasource-micrometer**  
Micrometer observability instrumentation for JDBC DataSource.

**datasource-micrometer-spring-boot**  
Spring Boot 3.x AutoConfiguration for micrometer observability.

### Maven Coordinates
**datasource-micrometer**

```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer</artifactId>
    <version>...</version>
</dependency>
```

**datasource-micrometer-spring-boot**

```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer</artifactId>
    <version>...</version>
</dependency>
```


## Java versions

The produced jars support following JDK versions at runtime:

- `datasource-micrometer`: Java 8+ to match with micrometer 1.x java baseline.
- `datasource-micrometer-spring-boot`: Java 17+ to match with Spring Boot 3.x java baseline.

## Documentation

[//]: # (- Current release)
[//]: # (    - [Reference Doc.][reference-current])
[//]: # (    - [API Doc.][javadoc-current])
[//]: # (    - [Changelog][changelog-current])
- Snapshot
    - [Reference Doc.][reference-snapshot]
    - [API Doc.][javadoc-snapshot]
    - [Changelog][changelog-snapshot]
- Other versions (TBD)


[reference-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/docs/html
[reference-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/docs/html
[javadoc-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/api/
[javadoc-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/api/
[changelog-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/CHANGELOG.txt
[changelog-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/CHANGELOG.txt

## Development

To build the entire project, JDK-17 is required.

```shell
./mvnw install
```

### Building document

```shell
./mvnw install -Pdocs -pl docs
```

### Building javadoc

```shell
./mvnw javadoc:aggregate
```

### Apply Source Code Format

```shell
./mvnw spring-javaformat:apply
```
