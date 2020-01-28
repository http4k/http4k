package org.http4k.security

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.junit.jupiter.api.Test

internal class AccessTokenFetcherTest {
    private val config = OAuthProviderConfig(Uri.of("irrelevant"), "/", "/path", Credentials("", ""))
    private val accessTokenFetcherAuthenticator = ClientSecretAccessTokenFetcherAuthenticator(config)

    @Test
    fun `can get access token from plain text body`() {
        val api = { _: Request -> Response(OK).body("some-access-token") }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }


    @Test
    fun `can get access token from json body`() {
        //see https://tools.ietf.org/html/rfc6749#section-4.1.4
        val api = { _: Request -> Response(OK).with(accessTokenResponseBody of AccessTokenResponse("some-access-token")) }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }

    @Test
    fun `can get access token from json body for content-type without directive`() {
        val api = { _: Request ->
            Response(OK)
                .header("Content-Type", "application/json")
                .body("{\"access_token\": \"some-access-token\"}")
        }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), equalTo(AccessTokenDetails(AccessToken("some-access-token"))))
    }

    @Test
    fun `handle non-successful response`() {
        val api = { _: Request -> Response(BAD_REQUEST) }

        val fetcher = AccessTokenFetcher(api, Uri.of("irrelevant"), config, accessTokenFetcherAuthenticator)

        assertThat(fetcher.fetch("some-code"), absent())
    }
}
