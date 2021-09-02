package org.http4k.security.oauth.client

import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenResponse
import java.time.Duration
import java.util.*

class FakeOauthServer(
    tokenPath: String,
    private val duration: Duration,
    private vararg val refreshTokens: String
): HttpHandler {

    val refreshHistory = mutableListOf<Pair<String, String>>()
    private val handler = routes(
        tokenPath bind Method.POST to ::tokenHandler
    )

    override fun invoke(request: Request) = handler(request)

    private fun tokenHandler(request: Request): Response {
        val data = OAuthOfflineRequestAuthorizer.tokenRequestLens(request)

        if (data.refresh_token !in refreshTokens) {
            return Response(Status.UNAUTHORIZED)
        }

        val responseData = AccessTokenResponse(
            access_token = UUID.randomUUID().toString(),
            expires_in = duration.seconds,
            refresh_token = data.refresh_token,
            token_type = "access_token"
        )
        refreshHistory += data.refresh_token!! to responseData.access_token

        return Response(Status.OK)
            .with(OAuthOfflineRequestAuthorizer.tokenResponseLens of responseData)
    }
}
