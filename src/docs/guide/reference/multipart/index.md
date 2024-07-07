title: http4k Multipart Module
description: Feature overview of the http4k-multipart form module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.25.0.0"))
    implementation("org.http4k:http4k-multipart")
}
```

### About

Multipart form support for fields and files, including a set of lens extensions for fields/files.

See the [how-to guides](/guide/howto/use_multipart_forms/) for example use.

### Receiving Binary content with http4k Contracts

With binary attachments, you need to turn ensure that the pre-flight validation does not eat the stream. This is possible by instructing http4k to ignore the incoming body for validation purposes:

```kotlin
routes += "/api/document-upload" meta {
    preFlightExtraction = PreFlightExtraction.IgnoreBody
} bindContract POST to { req -> Response(OK) }
```
