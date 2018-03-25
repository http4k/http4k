package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.query
import java.time.Clock


fun OAuth.Companion.dropbox(client: HttpHandler, credentials: Credentials, callbackUri: Uri, clock: Clock = Clock.systemUTC()) =
    OAuth(
        client,
        OAuthConfig("Dropbox",
            Uri.of("https://www.dropbox.com"),
            "/oauth2/authorize",
            "/oauth2/token",
            credentials, Uri.of("https://api.dropboxapi.com")),
        callbackUri,
        listOf(""),
        clock)

fun OAuth.Companion.google(client: HttpHandler, credentials: Credentials, callbackUri: Uri, scopes: List<String> = listOf("openid"), clock: Clock = Clock.systemUTC()) =
    OAuth(
        client,
        OAuthConfig("Google",
            Uri.of("https://accounts.google.com"),
            "/o/oauth2/v2/auth",
            "/oauth2/v4/token",
            credentials,
            Uri.of("https://www.googleapis.com")),
        callbackUri,
        scopes,
        clock,
        modifyAuthRedirect = { it.query("nonce", SECURE_GENERATE_RANDOM()) }
    )

fun OAuth.Companion.soundcloud(client: HttpHandler, credentials: Credentials, callbackUri: Uri, clock: Clock = Clock.systemUTC()) =
    OAuth(
        client,
        OAuthConfig("Soundcloud",
            Uri.of("https://soundcloud.com"),
            "/connect",
            "/oauth2/token",
            credentials,
            Uri.of("https://api.soundcloud.com")),
        callbackUri,
        listOf(""),
        clock
    )