package org.http4k.contract.security

import com.natpryce.hamkrest.assertion.assertThat
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.RequestContextKey
import org.http4k.security.jwt.JwtAuthorizer
import org.http4k.security.jwt.RsaProvider
import org.junit.jupiter.api.Test

class JwtSecurityTest {
    private val rsa = RsaProvider("testServer")

    private val authorizer = JwtAuthorizer(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.publicKey),
        lookup = { it.subject.toIntOrNull() }
    )

    private val requestContexts = RequestContexts()
    private val principal = RequestContextKey.required<Int>(requestContexts)

    private val http = ServerFilters.InitialiseRequestContext(requestContexts).then(
        contract {
            security = JwtSecurity(authorizer, principal)
            routes += "/" bindContract GET to { req: Request ->
                Response(OK).body(principal(req).toString())
            }
        }
    )

    @Test
    fun `valid jwt`() {
        val token = rsa.generate("1337")

        val response = Request(GET, "/")
            .header("Authorization", "Bearer $token")
            .let(http)

        assertThat(response, hasStatus(OK))
        assertThat(response, hasBody("1337"))
    }

    @Test
    fun `unauthorized jwt`() {
        val token = rsa.generate("foobarbaz")

        val response = Request(GET, "/")
            .header("Authorization", "Bearer $token")
            .let(http)

        assertThat(response, hasStatus(UNAUTHORIZED))
    }
}
