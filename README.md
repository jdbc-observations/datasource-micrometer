# Datasource Micrometer

[![CI](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml/badge.svg)](https://github.com/jdbc-observations/datasource-micrometer/actions/workflows/ci.yml?event=push&branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/net.ttddyy.observation/datasource-micrometer)](https://central.sonatype.com/artifact/net.ttddyy.observation/datasource-micrometer)

**Datasource Micrometer** adds [Micrometer Observation API][micrometer-observation] instrumentation for JDBC operations.

[micrometer-observation]: https://docs.micrometer.io/micrometer/reference/observation.html

---

## Modules

| Module                                  | Purpose                                                         |
|-----------------------------------------|-----------------------------------------------------------------|
| **datasource-micrometer**               | Micrometer observability instrumentation for JDBC `DataSource`. |
| **datasource-micrometer-opentelemetry** | OpenTelemetry semantic conventions support.                     |
| **datasource-micrometer-spring-boot**   | Spring Boot auto-configuration and related support.             |
| **datasource-micrometer-bom**           | Bill of Materials (BOM) for aligned versions of all modules.    |

---

## Dependencies

Replace `...` with the [version from Maven Central](https://central.sonatype.com/artifact/net.ttddyy.observation/datasource-micrometer).

### datasource-micrometer

**Maven**

```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer</artifactId>
    <version>...</version>
</dependency>
```

**Gradle**

```groovy
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer:..."
}
```

### datasource-micrometer-opentelemetry

**Maven**

```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-opentelemetry</artifactId>
    <version>...</version>
</dependency>
```

**Gradle**

```groovy
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer-opentelemetry:..."
}
```

### datasource-micrometer-spring-boot

**Maven**

```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>...</version>
</dependency>
```

**Gradle**

```groovy
dependencies {
    implementation "net.ttddyy.observation:datasource-micrometer-spring-boot:..."
}
```

### datasource-micrometer-bom

Import the BOM to align transitive versions (optional but recommended when you use multiple artifacts).

**Maven**

```xml
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

**Gradle**

```groovy
dependencies {
  implementation platform("net.ttddyy.observation:datasource-micrometer-bom:...")
}
```

---

## Snapshot releases

Add the Maven Central Portal Snapshots repository, then use a snapshot version as usual.

**Maven**

```xml
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

**Gradle**

```groovy
repositories {
    maven {
        name = 'Central Portal Snapshots'
        url = 'https://central.sonatype.com/repository/maven-snapshots/'

        // Only resolve datasource-micrometer artifacts from this repo
        content {
            includeModule("net.ttddyy.observation", "<datasource-micrometer artifacts>")
        }
    }
    mavenCentral()
}
```

Full setup details: [Consuming snapshot releases](https://central.sonatype.org/publish/publish-portal-snapshots/#consuming-snapshot-releases-for-your-project) (Sonatype).

---

## Java versions

Runtime JDK requirements by artifact:

| Artifact                            | JDK                                    |
|-------------------------------------|----------------------------------------|
| `datasource-micrometer`             | **8+** (aligned with Micrometer 1.x)   |
| `datasource-micrometer-spring-boot` | **17+** (aligned with Spring Boot 3.x) |

---

## Documentation

| Channel             | Reference                  | API                         | Changelog                       |
|---------------------|----------------------------|-----------------------------|---------------------------------|
| **Current release** | [HTML][reference-current]  | [Javadoc][javadoc-current]  | [CHANGELOG][changelog-current]  |
| **Snapshot**        | [HTML][reference-snapshot] | [Javadoc][javadoc-snapshot] | [CHANGELOG][changelog-snapshot] |

Other versions: *TBD.*

[reference-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/docs/html
[reference-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/docs/html
[javadoc-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/api/
[javadoc-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/api/
[changelog-current]: https://jdbc-observations.github.io/datasource-micrometer/docs/current/CHANGELOG.txt
[changelog-snapshot]: https://jdbc-observations.github.io/datasource-micrometer/docs/current-snapshot/CHANGELOG.txt

---

## Compatible versions

### Spring Boot (summary)

| Datasource Micrometer | Spring Boot |
|:---------------------:|:-----------:|
|          2.x          |     4.x     |
|          1.x          |     3.x     |

### Spring Boot, Micrometer BOM, and Micrometer Tracing BOM

| Datasource Micrometer |   Spring Boot    | Micrometer Tracing BOM | Micrometer BOM |
|:---------------------:|:----------------:|:----------------------:|:--------------:|
|          2.x          |       4.x        |         1.6.x          |     1.16.x     |
|       2.0.0-RC1       | 4.0.0-[M3,R1,R2] |     1.6.0-[M3,R1]      | 1.16.0-[M3,R1] |
|         1.0.0         |   3.0.0 and up   |         1.0.0          |     1.10.2     |
|       1.0.0-RC1       |    3.0.0-RC1     |       1.0.0-RC1        |   1.10.0-RC1   |
|       1.0.0-M1        |     3.0.0-M6     |        1.0.0-M8        |   1.10.0-M5    |

---

## Development

JDK **17** is required to build the project.

| Task                     | Command                          |
|--------------------------|----------------------------------|
| Full build               | `./mvnw install`                 |
| Documentation site       | `./mvnw install -Pdocs -pl docs` |
| Aggregated Javadoc       | `./mvnw javadoc:aggregate`       |
| Apply source formatting  | `./mvnw spring-javaformat:apply` |
