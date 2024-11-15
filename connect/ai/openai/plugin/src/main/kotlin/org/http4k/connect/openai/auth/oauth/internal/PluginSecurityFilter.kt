package org.http4k.connect.openai.auth.oauth.internal

import org.http4k.connect.openai.auth.oauth.PrincipalTokens
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.AccessToken

internal fun <Principal : Any> PluginSecurityFilter(accessTokens: PrincipalTokens<Principal>) =
    Filter { next ->
        {
            when (it.accessToken()?.let(accessTokens::resolve) != null) {
                true -> next(it)
                else -> Response(UNAUTHORIZED)
            }
        }
    }

fun Request.accessToken() = header("Authorization")
    ?.trim()
    ?.takeIf { it.startsWith("Bearer") }
    ?.substringAfter("Bearer")
    ?.trim()
    ?.let { AccessToken(it) }

