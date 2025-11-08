# Datasource Micrometer
[![CI](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml?event=push&branch=main)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.ttddyy.observation/datasource-micrometer/badge.svg)](https://search.maven.org/search?q=net.ttddyy.observation)

The Datasource Micrometer provides [Micrometer Observation API][micrometer-observation] instrumentation for JDBC operations.

[micrometer-observation]: https://docs.micrometer.io/micrometer/reference/observation.html

## Modules

**datasource-micrometer**  
Micrometer observability instrumentation for JDBC DataSource.

**datasource-micrometer-spring-boot**  
Spring Boot 3.x AutoConfiguration for micrometer observability.

### Dependency Settings
**datasource-micrometer**

```xml
<!-- Maven -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer</artifactId>
    <version>...</version>
</dependency>
```

```groovy
// Gradle
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer:..."
}
```

**datasource-micrometer-spring-boot**

```xml
<!-- Maven -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>...</version>
</dependency>
```
```groovy
// Gradle
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer-spring-boot:..."
}
```

### Using Snapshot

You need add Sonatype Snapshot repositories.

```xml
<!-- Maven -->
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <name>Sonatype Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

```groovy
// Gradle
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
}
```

## Java versions

The produced jars support following JDK versions at runtime:

- `datasource-micrometer`: Java 8+ to match with micrometer 1.x java baseline.
- `datasource-micrometer-spring-boot`: Java 17+ to match with Spring Boot 3.x java baseline.

## Documentation

- Current release
    - [Reference Doc.][reference-current]
    - [API Doc.][javadoc-current]
    - [Changelog][changelog-current]
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

## Dependent Library Versions

| DataSource Micrometer |   Spring Boot    | Micrometer Tracing BOM | Micrometer BOM |
|:---------------------:|:----------------:|:----------------------:|:--------------:|
|      `2.0.0-RC1`      | 4.0.0-[M3,R1,R2] |     1.6.0-[M3,R1]      | 1.16.0-[M3,R1] | 
|        `1.0.0`        |  3.0.0 and up    |         1.0.0          |     1.10.2     | 
|      `1.0.0-RC1`      |    3.0.0-RC1     |       1.0.0-RC1        |   1.10.0-RC1   | 
|      `1.0.0-M1`       |     3.0.0-M6     |        1.0.0-M8        |   1.10.0-M5    | 

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
