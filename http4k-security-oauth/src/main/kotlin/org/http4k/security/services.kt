package org.http4k.security

import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.security.CrossSiteRequestForgeryToken.Companion.SECURE_CSRF

fun OAuthProvider.Companion.dropbox(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence): OAuthProvider =
    OAuthProvider(
        OAuthProviderConfig(Uri.of("https://www.dropbox.com"), "/oauth2/authorize", "/oauth2/token", credentials, Uri.of("https://api.dropboxapi.com")),
        client,
        callbackUri,
        listOf(""),
        oAuthPersistence)

fun OAuthProvider.Companion.google(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence, scopes: List<String> = listOf("openid")): OAuthProvider =
    OAuthProvider(
        OAuthProviderConfig(Uri.of("https://accounts.google.com"), "/o/oauth2/v2/auth", "/oauth2/v4/token", credentials, Uri.of("https://www.googleapis.com")),
        client,
        callbackUri,
        scopes,
        oAuthPersistence,
        { it.query("nonce", SECURE_CSRF().value) },
        SECURE_CSRF
    )

fun OAuthProvider.Companion.soundCloud(client: HttpHandler, credentials: Credentials, callbackUri: Uri, oAuthPersistence: OAuthPersistence): OAuthProvider =
    OAuthProvider(
        OAuthProviderConfig(Uri.of("https://soundcloud.com"), "/connect", "/oauth2/token", credentials, Uri.of("https://api.soundcloud.com")),
        client,
        callbackUri,
        listOf(""),
        oAuthPersistence
    )