package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.time.Clock

fun OAuth.Companion.google(client: HttpHandler, credentials: Credentials, callbackUri: Uri, scopes: List<String> = listOf("openid", "email", "profile"), clock: Clock = Clock.systemUTC()) =
    OAuth(
        client,
        OAuthConfig("Google",
            Uri.of("https://accounts.google.com"),
            "/o/oauth2/v2/auth",
            Uri.of("https://www.googleapis.com"),
            "/oauth2/v4/token",
            credentials),
        callbackUri,
        scopes,
        clock
    )