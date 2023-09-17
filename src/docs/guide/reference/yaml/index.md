title: http4k YAML Message Format Modules
description: Feature overview of the YAML http4k-format modules, several of which support auto-marshalling into data classes.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.8.1.0"))
    implementation("org.http4k:http4k-format-jackson-yaml")
    implementation("org.http4k:http4k-format-moshi-yaml")
}
```

### About
These modules add the ability to use YAML as a first-class citizen when reading from and to HTTP messages. 

[http4k]: https://http4k.org
