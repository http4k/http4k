title: http4k Custom OAuth Provider
description: Recipe for using http4k with custom OAuth provider

It is very easy to configure http4k to integrate with any OAuth2 provider who supports the Authorisation Code Grant.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "3.263.0"
implementation group: "org.http4k", name: "http4k-security-oauth", version: "3.263.0"
```

For this example, simply reconfigure the `OAuthProvider` instance with the correct details, and provide custom logic for persisting and retrieving the CSRF and AccessToken.

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/custom_oauth_provider/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/custom_oauth_provider/example.kt"></script>
