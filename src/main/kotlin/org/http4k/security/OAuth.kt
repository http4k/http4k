package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

/**
 * POST www.googleapis.com/oauth2/v4/token
Content-Type: application/x-www-form-urlencoded
code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7&
client_id=8819981768.apps.googleusercontent.com&
client_secret={client_secret}&
redirect_uri=https://oauth2-login-demo.example.com/code&
grant_type=authorization_code
 */
/**

 *     https://accounts.google.com/o/oauth2/v2/auth?
client_id=424911365001.apps.googleusercontent.com&
response_type=code&
scope=openid%20email&
redirect_uri=http://localhost:9000/callback
state=below
encode!!    security_token=138r5719ru3e1&url=https://oauth2-login-demo.example.com/myHome&
nonce=random&
 */


class OAuth(client: HttpHandler,
            private val clientConfig: OAuthClientConfig,
            private val callbackUri: Uri,
            private val scopes: List<String>,
            private val generateCrsf: () -> String = { BigInteger(130, SecureRandom()).toString(32) },
            private val generateNonce: () -> String = { UUID.randomUUID().toString() }) {

    private val crsfCookieName = "csrf"
    private val tokenCookieName = "accessToken"

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter = object : Filter {
        override fun invoke(next: HttpHandler): HttpHandler {
            return { req ->
                req.cookie(tokenCookieName)?.let { next(req) } ?: redirectToAuth(req.uri)
            }
        }
    }

    val callback: HttpHandler = {
        //http://localhost:9000/callback?state=/?csrf%3Dehnst6i3n89khhn0v27ks6t8gn&code=4/AABog4Jo0BMKTmTKZR87LnGES1U4Q2deF6MbBMvqs4fiDKhTZr0LT0GPYAZ-mBX2gO8JwJZVlvjQ9k_D50K1MIg&authuser=0&session_state=7c0f1ea0f7dff7252ed75788864337e165d75232..05be&prompt=consent#
        it.query("code")
            ?.let { code -> it.cookie(crsfCookieName)?.let { code to it } }
            ?.let { (code, csrfCookie) ->
                println("hit callback$ $code, $csrfCookie")
                when {
                    csrfCookie.value != it.query("csrf") -> Response(Status.FORBIDDEN).invalidateCookie(crsfCookieName)
                    else -> codeToAccessToken(code, Uri.of(""))
                }
            } ?: Response(Status.FORBIDDEN)
    }

    private fun codeToAccessToken(code: String, originalUri: Uri): Response {
        val accessToken = api(Request(Method.POST, clientConfig.tokenPath)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", originalUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))

        println(accessToken.bodyString())
        return Response(Status.TEMPORARY_REDIRECT).cookie(Cookie(tokenCookieName, accessToken.bodyString()))
    }

    private fun redirectToAuth(originalUri: Uri) = generateCrsf().let {
        Response(Status.TEMPORARY_REDIRECT).with(Header.Common.LOCATION of clientConfig.authUri
            .query("client_id", clientConfig.credentials.user)
            .query("response_type", "code")
            .query("scope", scopes.joinToString(" "))
            .query("redirect_uri", callbackUri.toString())
            .query("state", originalUri.query(crsfCookieName, it).toString())
            .query("nonce", generateNonce()))
            .cookie(Cookie(crsfCookieName, it))
    }

    companion object
}