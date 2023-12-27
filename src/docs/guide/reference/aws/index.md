title: http4k AWS Module
description: Feature overview of the http4k-aws module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.11.1.0"))
    implementation("org.http4k:http4k-aws")
}
```

### About
This module provides 2 things: a http4k compatible `SdkHttpClient` and a super-simple AWS request signing functionality for talking to AWS services.

1. With the `SdkHttpClient` you can use the standard Amazon SDKs libraries by plugging in a standard `HttpHandler`. This simplifies fault testing and means that you can print out the exact traffic which is going to AWS - which is brilliant for both debugging and writing Fakes. :)

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/aws/example_sdk.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/aws/example_sdk.kt"></script>

2. With the request signing functionality, once configured with the correct keys, the various AWS services are actually really simple to integrate with. They're just RESTy-type HTTPS services - the main difficulty is that all requests need to have their contents digitally signed with the AWS credentials to be authorised.

http4k provides a `Filter` which does this request signing process. Just decorate a standard HTTP client and then make the relevant calls:


#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/aws/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/aws/example.kt"></script>
