package org.http4k.connect.amazon.cognito.oauth

import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.oauth.Form.email
import org.http4k.connect.amazon.cognito.oauth.Form.formLens
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.FormField
import org.http4k.lens.Header.AUTHORIZATION_BASIC
import org.http4k.lens.Header.LOCATION
import org.http4k.lens.Validator.Strict
import org.http4k.lens.webForm
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.oauth.server.OAuthServer
import org.http4k.security.oauth.server.OAuthServer.Companion.clientIdQueryParameter
import java.time.Clock
import java.time.Duration

fun CognitoOAuth(pools: Storage<CognitoPool>, clock: Clock, expiry: Duration): RoutingHttpHandler {
    val server = OAuthServer(
        "/oauth2/token",
        InMemoryAuthRequestTracking(),
        CognitoPoolClientValidator(pools),
        InMemoryAuthorizationCodes(clock),
        CognitoAccessTokens(clock, expiry, Region.of("ldn-north-1")),
        clock
    )

    return CatchLensFailure()
        .then(
            routes(
                CopyNonStandardOAuthParamsIntoFormIfItIsNotAlreadyThere().then(server.tokenRoute),
                "/oauth2/authorize" bind GET to server.authenticationStart.then {
                    Response(FOUND).with(LOCATION of it.uri.path("/oauth2/login"))
                },
                "/oauth2/login" bind routes(
                    GET to server.authenticationStart.then {
                        Response(OK).body(LoginPage(pools.findMatchingClientName(it)))
                    },
                    POST to { request ->
                        if (email(formLens(request)).contains('@')) {
                            server.authenticationComplete(request)
                        } else Response(SEE_OTHER).with(LOCATION of request.uri)
                    }
                )
            )
        )
}

private fun CopyNonStandardOAuthParamsIntoFormIfItIsNotAlreadyThere() = Filter { next ->
    {
        next(
            it
                .form("grant_type", it.form("grant_type") ?: it.query("grant_type") ?: "")
                .form("client_id", it.form("client_id") ?: AUTHORIZATION_BASIC(it)?.user ?: "")
                .form("client_secret", it.form("client_secret") ?: AUTHORIZATION_BASIC(it)?.password ?: "")
        )
    }
}

private fun Storage<CognitoPool>.findMatchingClientName(
    req: Request
) = (keySet()
    .firstNotNullOfOrNull {
        this[it]
            ?.clients
            ?.firstOrNull { it.ClientId.value == clientIdQueryParameter(req).value }
    }
    ?.ClientName
    ?: ClientName.of("Cognito"))

internal object Form {
    val email = FormField.required("email")
    val formLens = Body.webForm(Strict, email).toLens()
}

private fun LoginPage(name: ClientName) = """
    <html>
    <head> 
        <title>${name} Login</title> 
    </head>
    <body>
    Enter your email to log into <b>${name}</b>.
    <i>You will be issued a JWT with the verified email<./i>
    <form id="loginForm" method="POST">
        <input id="email" type="text" placeholder="Enter email" name="email"><br>
        <button type="submit">Login</button>
    </form>
    </body>
    </html>
"""
