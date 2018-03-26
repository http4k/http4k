package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.query

fun OAuth.Companion.dropbox(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence): OAuth {
    val config = OAuthConfig(Uri.of("https://www.dropbox.com"), "/oauth2/authorize", "/oauth2/token", credentials, Uri.of("https://api.dropboxapi.com"))

    return OAuth(
        client,
        config,
        callbackUri,
        listOf(""),
        oAuthPersistence)
}

fun OAuth.Companion.google(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence, scopes: List<String> = listOf("openid")): OAuth {
    val clientConfig = OAuthConfig(Uri.of("https://accounts.google.com"), "/o/oauth2/v2/auth", "/oauth2/v4/token", credentials, Uri.of("https://www.googleapis.com"))

    return OAuth(
        client,
        clientConfig,
        callbackUri,
        scopes,
        oAuthPersistence,
        { it.query("nonce", SECURE_GENERATE_RANDOM()) },
        SECURE_GENERATE_RANDOM
    )
}

fun OAuth.Companion.soundCloud(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence): OAuth {
    val clientConfig = OAuthConfig(Uri.of("https://soundcloud.com"), "/connect", "/oauth2/token", credentials, Uri.of("https://api.soundcloud.com"))

    return OAuth(
        client,
        clientConfig,
        callbackUri,
        listOf(""),
        oAuthPersistence
    )
}