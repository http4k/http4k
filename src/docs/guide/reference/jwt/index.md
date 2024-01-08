title: http4k JWT security Module
description: Feature overview of the http4k-security-jwt module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.12.1.0"))
    implementation("org.http4k:http4k-security-jwt")
}
```

### About

`JWTs` are cryptographic access tokens containing easily obtainable information on the `subject` (i.e. user/principal), and can be securely verified with minimal participation from the `login provider`.
`JWTs` have a limited lifetime, and must be periodically refreshed.

They are exceptionally well suited to social login.
The social `login provider` can cryptographically sign the `JWT` with an asymmetric key, including a confidential private key, and freely available set of rotating public keys.
Third-party resource servers can cryptographically verify the `JWT` using the public key set, eliminating the need for their own custom `login provider`.
The public key can be cached for long periods of time, which reduces the burden on the `login provider`, and latency to authorize requests.

At it's most basic, JWT authorization typically works like this:

1. Client authenticates with a `login provider` and is issued a `JWT`
2. Client can inspect the `JWT` for information on the `subject` (e.g. username, email, photo, etc.)
3. Client can make a request to a `resource server`, including the `JWT` in the `Authorization` header
4. Server cryptographically verifies the `JWT` is valid and was issued by one of its trusted `login providers`
5. Server extracts the `subject` ID from the `JWT`, and uses it to complete the request

This module contains the infrastructure required for a `resource server` to authorize requests containing a `JWT`.


### Simple Filter [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/simple_filter.kt)

This minimal example will authorize any request with a valid JWT.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/simple_filter.kt"></script>

### Advanced Filter  [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/advanced_filter.kt)

This example will display some of the more advanced capabilities:

- Transform Subject into any principal type
- Perform additional verification on the subject
- Inject verified subject into a `RequestContextLens`

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/advanced_filter.kt"></script>

### Testability [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/testability.kt)

For apps that use a remote JWK to load public keys, it may be common for tests to replace the JWK with a local key.
However, this requires injecting fake components into the app factory method.
This module provides a universal component that can retrieve a JWK from both a real and fake server.

The `http4kJwsKeySelector` requires you to inject the internet as an `HttpHandler`, which can be faked to return an in-memory JWK.
With this components, the only difference between a test and production app is the internet that you inject. 

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/testability.kt"></script>

### Contract Security  [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/security.kt)

There is also support for `http4k-contract` with the `JwkSecurity` class.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jwt/security.kt"></script>
