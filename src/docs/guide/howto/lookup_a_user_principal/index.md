title: http4k How-to: Lookup a user principal
description: Recipes for looking up and populating a user principal from a request


### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.16.0.0"
```

When authorising requests, it is common to need to store some credentials or a user principal object to be accessible by a further Filter or the eventual HttpHandler.

This can be easily achieved by combining the typesafe RequestContext functionality with one of the built-in authorisation Filters:

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/lookup_a_user_principal/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/lookup_a_user_principal/example.kt"></script>
