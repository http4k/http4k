title: http4k Digest security Module
description: Feature overview of the http4k-security-digest module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-security-digest", version: "4.32.3.0"
```

### About

Support for integrating with servers secured by Digest authentication; useful for working with legacy
servers or IOT devices that don't typically support TLS.  For completeness, a Digest Provider has also been included for use with servers.

Digest authentication is useful for protecting credentials in transit when traffic isn't encrypted.
Instead of the client transmitting plain-text or encrypted credentials, it sends a hash of the credentials instead; this ensures
a man-in-the-middle can never intercept the credentials, despite the connection being insecure.

Despite being made redundant by TLS, digest authentication has a major disadvantage; it typically requires user credentials
to be accessible by the server.  In most other authentication mechanisms, the server can store a non-reversible hash, which reduces the severity of a database breach.

At it's most basic, the digest authentication flow works like this:

1. Client makes an HTTP call to a server protected by Digest authentication
2. The server responds with an `HTTP 401`, including a `Digest` challenge in the `WWW-Authenticate` header.
This header includes all the information the client needs to correctly generate a credentials `hash`
3. With the user-supplied credentials, the client converts them into `hash` and encodes them as a hexadecimal `digest`,
then transmits them to the server, along with the plaintext `username`
4. With the `username` given by the client, the server looks up the `password` for that user, generates the expected `hash`,
   and compares is to the one supplied by the client.  If they match, it grants the client access to the protected resource


### Example Provider [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/digest/example_provider_digest.kt)

This example has an integrated username/password store; you will want to come up with your own version, with credentials encrypted at rest.

The server accepts a path parameter, and parrots back the value provided by the client.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/digest/example_provider_digest.kt"></script>

### Example Client  [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/digest/example_client_digest.kt)

This example integrates with the provider above, sending a request with a value to be parroted back.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/digest/example_client_digest.kt"></script>
