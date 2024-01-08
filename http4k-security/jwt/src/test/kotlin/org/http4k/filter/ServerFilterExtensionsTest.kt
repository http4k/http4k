package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.RequestContextKey
import org.http4k.security.jwt.JwtAuthorizer
import org.http4k.security.jwt.RsaProvider
import org.junit.jupiter.api.Test

class ServerFilterExtensionsTest {
    private val rsa = RsaProvider("testServer")
    private val authorizer = JwtAuthorizer(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
        lookup = { if (it.subject == "user1") 1 else null }
    )

    private val requestContexts = RequestContexts()
    private val principal = RequestContextKey.required<Int>(requestContexts)

    private val http = ServerFilters.InitialiseRequestContext(requestContexts)
        .then(ServerFilters.JwtAuth(authorizer, principal))
        .then { req: Request -> Response(OK).body(principal(req).toString()) }

    @Test
    fun `missing token`() {
        val response = Request(Method.GET, "/")
            .let(http)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `invalid token`() {
        val response = Request(Method.GET, "/")
            .header("Authorization", "Bearer foo")
            .let(http)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `valid token but unauthorized user`() {
        val token = rsa.generate("foo")

        val response = Request(Method.GET, "/")
            .header("Authorization", "Bearer $token")
            .let(http)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }

    @Test
    fun `valid token and authorized user`() {
        val token = rsa.generate("user1")

        val response = Request(Method.GET, "/")
            .header("Authorization", "Bearer $token")
            .let(http)

        assertThat(response, hasStatus(OK))
        assertThat(response, hasBody("1"))
    }
}
