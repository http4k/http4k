package guide.reference.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.proc.SingleKeyJWSKeySelector
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.JwtAuth
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextKey
import org.http4k.security.jwt.JwtAuthorizer
fun main() {
    // generate a new RSA key pair
    val rsa = RsaProvider()

    val userWhitelist = setOf(1, 2)

    // authorize requests using the RSA public key
    val authorizer = JwtAuthorizer(
        keySelector = SingleKeyJWSKeySelector(JWSAlgorithm.RS256, rsa.public),
        // transform the subject into an Int and check if it's in the whitelist
        lookup = { claims -> claims.subject.toIntOrNull()?.takeIf { it in userWhitelist } }
    )

    // store the authenticated principal in a lens
    val requestContexts = RequestContexts()
    val userIdLens = RequestContextKey.required<Int>(requestContexts)

    // Build a server protected by the JwtAuthorizer
    val http = ServerFilters.InitialiseRequestContext(requestContexts)
        // the JWT filter will store the authorized principal in a lens
        .then(ServerFilters.JwtAuth(authorizer, userIdLens))
        // server will return the user's id in the response body
        .then { request -> Response(OK).body(userIdLens(request).toString()) }

    // invalid user ids will be rejected
    val invalidSubjectRequest = Request(GET, "/")
        .header("Authorization", "Bearer ${rsa.newJwt("user1")}")
    println(http(invalidSubjectRequest)) // UNAUTHORIZED

    // user ids not in the whitelist will be rejected
    val forbiddenUserRequest = Request(GET, "/")
        .header("Authorization", "Bearer ${rsa.newJwt("1")}")
    println(http(forbiddenUserRequest)) // UNAUTHORIZED

    // the whitelisted user will be permitted and its userId returned in the response
    val authorizedRequest = Request(GET, "/")
        .header("Authorization", "Bearer ${rsa.newJwt("2")}")
    println(http(authorizedRequest))  // OK user1
}
