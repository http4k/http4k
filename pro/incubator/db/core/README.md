# http4k Database

This module provides a simple abstraction to connect to databases and manage transactions in a consistent and testable way.


## Getting started

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.46.0.0"))
    implementation("org.http4k:http4k-incubator-db-core")
}
```

## Usage

See [Example](../jdbc/src/test/kotlin/org/http4k/db/example/Example.kt)

## Testing

Some tests will only be executed if a database is available. You can use the following Docker commands to start PostgreSQL or MySQL instances for testing.

### Starting PostgreSQL for testing

```shell
docker run --name http4k-test-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres:17.2
```

### Starting MySQL for testing

```shell
docker run --name http4k-test-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysecretpassword -d mysql:9.2
```

