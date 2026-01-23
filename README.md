# Datasource Micrometer
[![CI](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml?event=push&branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/net.ttddyy.observation/datasource-micrometer)](https://central.sonatype.com/artifact/net.ttddyy.observation/datasource-micrometer)

The Datasource Micrometer provides [Micrometer Observation API][micrometer-observation] instrumentation for JDBC operations.

[micrometer-observation]: https://docs.micrometer.io/micrometer/reference/observation.html

## Modules

**datasource-micrometer**  
Micrometer observability instrumentation for JDBC DataSource.

**datasource-micrometer-opentelemetry**  
OpenTelemetry Semantic Conventions support.

**datasource-micrometer-spring-boot**  
Spring Boot support (auto-configurations, etc) for micrometer observability.

**datasource-micrometer-bom**  
Bill of Materials(BOM) for Datasource Micrometer modules.


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
**datasource-micrometer-opentelemetry**
```xml
<!-- Maven -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-opentelemetry</artifactId>
    <version>...</version>
</dependency>
```
```groovy
// Gradle
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer-opentelemetry:..."
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

**datasource-micrometer-bom**

```xml
<!-- Maven -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>net.ttddyy.observation</groupId>
      <artifactId>datasource-micrometer-bom</artifactId>
      <version>...</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```
```groovy
// Gradle
dependencies {
  implementation platform("net.ttddyy.observation:datasource-micrometer-bom:...")
}
```

### Using Snapshot

To use snapshot releases, add the Maven Central Portal Snapshots repository to your project configuration.

```xml
<!-- Maven -->
<repositories>
    <repository>
        <name>Central Portal Snapshots</name>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
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
        name = 'Central Portal Snapshots'
        url = 'https://central.sonatype.com/repository/maven-snapshots/'

        // Only search this repository for the specific dependency
        content {
            includeModule("net.ttddyy.observation", "<datasource-micrometer artifacts>")
        }
    }
    mavenCentral()
}
```
For full instructions, see [the official documentation.](https://central.sonatype.org/publish/publish-portal-snapshots/#consuming-snapshot-releases-for-your-project)

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

Spring Boot Support:

| DataSource Micrometer | Spring Boot |
|:---------------------:|:-----------:|
|         `2.x`         |     4.x     |
|         `1.x`         |     3.x     |


| DataSource Micrometer |   Spring Boot    | Micrometer Tracing BOM | Micrometer BOM |
|:---------------------:|:----------------:|:----------------------:|:--------------:|
|         `2.x`         |       4.x        |         1.6.x          |     1.16.x     | 
|      `2.0.0-RC1`      | 4.0.0-[M3,R1,R2] |     1.6.0-[M3,R1]      | 1.16.0-[M3,R1] | 
|        `1.0.0`        |   3.0.0 and up   |         1.0.0          |     1.10.2     | 
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
