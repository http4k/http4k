title: http4k OAuth security Module
description: Feature overview of the http4k-security-oauth form module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-security-oauth", version: "4.28.0.0"
```

### About

Support for using integrating with external OAuth2 providers for authentication purposes and to provide access to external APIs of entities such as Auth0, Google etc. 


Specifically, http4k supports the popular OAuth2 `Authorization Code` and `Refresh Token` Grants.

### Authorization Code Grant

This flow requires user interaction; it uses a callback mechanism that plays out like this:

1. App developer (you!) creates an application on the OAuth provider and receives a `Client Id` and a `Client Secret`. You also provide a "callback" URL to the provider which will be used later.
2. When accessing a protected resource, your app checks for an `AccessToken` from the user (via cookie or similar)
3. If the user has no token, the app redirects the user browser back to the OAuth provider site, along with the "state" of the user - containing a generated `CrossSiteRequestForgeryToken` (CSRF - which is also stored by the app) and the original URI the user was trying to access.
4. The user logs in on the OAuth provider site, which generates a code that is returned as a query parameter in a redirect back to the registered callback URL in your app, along with the CSRF token.
5. Your app checks the content of the CSRF token to determine that the redirect is genuine, then sends the received code back to the OAuth provider in exchange for a valid `AccessToken`. This completes the flow
6. The `AccessToken` can then be used to access various services from the OAuth provider APIs.

There is a single user-defined interface, `OAuthPersistence`, required to implement to enable this flow. This interface is required to provide the custom way in which your application will store and retrieve the `CSRF` and `AccessToken` for a request. A common way to do this is through Cookies, but the values should definitely be encrypted. http4k only provides an insecure version of this class that you can use for testing. In order to remain provider-agnostic, the AccessToken object also contains the entirety of the (typically JSON) token response from the provider, which may include other fields depending on the types of scope for which your application is authorised by the user.

To enable OAuth integration, construct a configured instance of `OAuthProvider`. This provides 3 things:

1. A filter to protect application resources
1. A callback HttpHandler for the OAuth provider to redirect the authenticated user to
1. A fully configured API client (which populated the Host on the URI) - this allows different
implementations of the provider to be used across environments.

#### Example provider [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/oauth/example_provider_oauth.kt)

Out of the box, http4k provides implementations for several OAuth providers.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/oauth/example_provider_oauth.kt"></script>

See the [how-to guides](/guide/howto/use_a_custom_oauth_provider/) for a custom implementation.

### Refresh Token Grant

This flow can be performed "offline", i.e. once a user has given their consent via the `Authorization Code` grant, your server can continue to access protected resources on behalf of the user after the original `AccessToken` expires.

The flow plays out like this:

1. After the user performs the `Authorization Code` grant, your server will store the `RefreshToken`
2. The next time you need to access a protected resource on behalf of the user, your server retrieves the user's `RefreshToken`, then uses the preconfigured `Client Id` and `Client Secret` to exchange the `RefreshToken` for a new `AccessToken`.
3. [Optional] Your server can cache the refreshed `AccessToken` if several calls need to be made in a short period of time

#### Example Filter [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/oauth/example_offline_oauth.kt)

Http4k provides a filter that can be attached to your client to authorize requests to the OAuth resource server.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/oauth/example_offline_oauth.kt"></script>


