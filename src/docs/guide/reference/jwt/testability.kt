package guide.reference.jwt

import com.natpryce.hamkrest.assertion.assertThat
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.JwtAuth
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.uri
import org.http4k.routing.reverseProxy
import org.http4k.security.jwt.JwtAuthorizer
import org.http4k.security.jwt.RsaProvider
import org.http4k.security.jwt.http4kJwsKeySelector
import org.http4k.security.jwt.jwkServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.Test

private val issuerKey = EnvironmentKey.uri().required("ISSUER")

// The app factory that retrieves the public key from a remote JWK
fun createApp(
    env: Environment,
    internet: HttpHandler
): HttpHandler {
    val authorizer = JwtAuthorizer(
        keySelector = http4kJwsKeySelector(
            jwkUri = env[issuerKey].path("jwk.json"),
            algorithm = JWSAlgorithm.RS256,
            http = internet
        ),
        exactMatchClaims = JWTClaimsSet.Builder()
            .issuer(env[issuerKey].toString())
            .build(),
        lookup = { it.subject }
    )

    return ServerFilters.JwtAuth(authorizer)
        .then { _: Request -> Response(OK) }
}

class AppTest {
    private val rsa = RsaProvider("exampleServer")

    // create a fake app
    private val app = createApp(
        // inject a fake issuer URI
        env = Environment.defaults(
            issuerKey of Uri.of("https://auth.fake")
        ),
        // override the internet, returning a JWK from a fake issuer
        internet = reverseProxy(
            "auth.fake" to jwkServer(
                RSAKey.Builder(rsa.publicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID("key1")
                    .build()
                    .toPublicJWK()
            )
        )
    )

    @Test
    fun `authorize request`() {
        // when the app authorizes the request, it loads the public key from the fake remote JWK
        val response = Request(Method.GET, "/")
            .header("Authorization", "Bearer ${rsa.generate("user1")}")
            .let(app)

        assertThat(response, hasStatus(OK))
    }
}

// when creating a real app, we use the exact same factory method, but with a real internet
fun main() {
    createApp(
        env = Environment.ENV,
        internet = JavaHttpClient()
    ).asServer(Jetty(8000))
        .start()
}
