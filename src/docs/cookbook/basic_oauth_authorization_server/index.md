title: http4k Custom OAuth Authorization Server
description: Recipe for using http4k to create an authorization server that provides an *authorization code* access flow

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.3.5.2"
implementation group: "org.http4k", name: "http4k-security-oauth", version: "4.3.5.2"
```

For this example, you need to configure `OAuthServer` instance with the correct implementations of your login pages, generation of authentication codes and access tokens.

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/basic_oauth_authorization_server/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/basic_oauth_authorization_server/example.kt"></script>
