package org.http4k.security

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.hamkrest.hasAuthority
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class OAuthProvidersTest {

    private val uri = Uri.of("https://foo")
    private val credentials = Credentials("user", "pass")
    private val oAuthPersistence = FakeOAuthPersistence()

    @Test
    fun `configures correctly`() {
        assertProvider(OAuthProvider.auth0(uri, client("foo"), credentials, uri, oAuthPersistence), "https://foo/authorize")
        assertProvider(OAuthProvider.dropbox(client("api.dropboxapi.com"), credentials, uri, oAuthPersistence), "https://www.dropbox.com/oauth2/authorize")
        assertProvider(OAuthProvider.facebook(client("graph.facebook.com"), credentials, uri, oAuthPersistence), "https://www.facebook.com/dialog/oauth")
        assertProvider(OAuthProvider.gitHub(client("github.com"), credentials, uri, oAuthPersistence), "https://github.com/login/oauth/authorize")
        assertProvider(OAuthProvider.google(client("www.googleapis.com"), credentials, uri, oAuthPersistence), "https://accounts.google.com/o/oauth2/v2/auth")
        assertProvider(OAuthProvider.soundCloud(client("api.soundcloud.com"), credentials, uri, oAuthPersistence), "https://soundcloud.com/connect")
    }

    private fun assertProvider(provider: OAuthProvider, expectedRedirectPrefix: String) {
        assertThat(provider.api(Request(GET, "/")), hasStatus(OK))
        assertThat(provider.authFilter.then { Response(OK) }(Request(GET, "/")),
            hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", startsWith(expectedRedirectPrefix)))
        )
    }

    private fun client(auth: String): HttpHandler = {
        assertThat(it.uri, hasAuthority(auth))
        Response(OK)
    }
}
