package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.Common.LOCATION
import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

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

    private val serviceCsrfName = "${clientConfig.serviceName}Csrf"
    private val serviceAccessTokenName = "${clientConfig.serviceName}AccessToken"

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter = object : Filter {
        override fun invoke(next: HttpHandler): HttpHandler {
            return { req ->
                req.cookie(serviceAccessTokenName)?.let { next(req) } ?: redirectToAuth(req.uri)
            }
        }
    }

    val callback: HttpHandler = {
        //http://localhost:9000/callback?state=/?csrf%3Dehnst6i3n89khhn0v27ks6t8gn&code=4/AABog4Jo0BMKTmTKZR87LnGES1U4Q2deF6MbBMvqs4fiDKhTZr0LT0GPYAZ-mBX2gO8JwJZVlvjQ9k_D50K1MIg&authuser=0&session_state=7c0f1ea0f7dff7252ed75788864337e165d75232..05be&prompt=consent#
        it.query("code")
            ?.let { code -> it.cookie(serviceCsrfName)?.let { code to it } }
            ?.let { (code, csrfCookie) ->
                println("hit callback$ $code, $csrfCookie")
                when {
                    csrfCookie.value != it.query(serviceCsrfName) -> Response(FORBIDDEN).invalidateCookie(serviceCsrfName)
                    else -> codeToAccessToken(code, Uri.of(""))
                }
            } ?: Response(FORBIDDEN)
    }

    private fun codeToAccessToken(code: String, originalUri: Uri): Response {
        val accessToken = api(Request(POST, clientConfig.tokenPath)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", originalUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))

        println(accessToken.bodyString())
        return Response(Status.TEMPORARY_REDIRECT).cookie(Cookie(serviceAccessTokenName, accessToken.bodyString()))
    }

    private fun redirectToAuth(originalUri: Uri) = generateCrsf().let {
        Response(Status.TEMPORARY_REDIRECT).with(LOCATION of clientConfig.authUri
            .query("client_id", clientConfig.credentials.user)
            .query("response_type", "code")
            .query("scope", scopes.joinToString(" "))
            .query("redirect_uri", callbackUri.toString())
            .query("state", originalUri.query(serviceCsrfName, it).toString())
            .query("nonce", generateNonce()))
            .cookie(Cookie(serviceCsrfName, it))
    }

    companion object
}