package guide.reference.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.JwtAuth
import org.http4k.filter.ServerFilters
import org.http4k.security.jwt.JwtAuthorizer

fun main() {
    // generate a new RSA key pair
    val rsa = RsaProvider()

    // authorize requests using the RSA public key
    val authorizer = JwtAuthorizer(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.public),
        lookup = { it.subject } // The principal is the JWT's subject
    )

    // Build a server protected by the JwtAuthorizer
    val http = ServerFilters.JwtAuth(authorizer)
        .then { _: Request -> Response(OK) }

    // requests without a valid JWT will be rejected
    val unauthorizedRequest = Request(GET, "/")
        .header("Authorization", "Bearer letmein")
    println(http(unauthorizedRequest)) // UNAUTHORIZED

    // requests with a valid JWT will be allowed
    val authorizedRequest = Request(GET, "/")
        .header("Authorization", "Bearer ${rsa.newJwt("user1")}")
    println(http(authorizedRequest))  // OK
}
