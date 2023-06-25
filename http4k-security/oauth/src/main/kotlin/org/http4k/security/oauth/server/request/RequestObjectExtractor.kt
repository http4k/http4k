package org.http4k.security.oauth.server.request

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.core.Uri
import org.http4k.security.oauth.format.OAuthMoshi
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidRequestObject
import org.http4k.security.oauth.format.boolean
import org.http4k.security.oauth.format.long
import org.http4k.security.oauth.format.map
import org.http4k.security.oauth.format.string
import org.http4k.security.oauth.format.strings
import org.http4k.security.oauth.format.value
import java.util.Base64

object RequestObjectExtractor {

    fun extractRequestJwtClaimsAsMap(value: String): Result<Map<*, *>, InvalidRequestObject> =
        parseJsonFromJWT(value)

    fun extractRequestObjectFromJwt(value: String): Result<RequestObject, InvalidRequestObject> =
        parseJsonFromJWT(value)
            .map { jsonFromJWT ->
                with(jsonFromJWT) {
                    RequestObject(
                        client = value("client_id", ::ClientId),
                        redirectUri = value("redirect_uri", Uri::of),
                        audience = toAudience(this["aud"]),
                        issuer = string("iss"),
                        scope = string("scope")?.split(" ") ?: emptyList(),
                        responseMode = value("response_mode", ResponseMode::fromQueryParameterValue),
                        responseType = value("response_type", ResponseType::fromQueryParameterValue),
                        state = value("state", ::State),
                        nonce = value("nonce") { org.http4k.security.Nonce(it) },
                        magAge = long("max_age"),
                        expiry = long("exp"),
                        claims = toClaims(this["claims"])
                    )
                }
            }

    @Suppress("UNCHECKED_CAST")
    private fun toClaims(claims: Any?) = when (claims) {
        is Map<*, *> -> Claims(
            asClaims(claims.map("userinfo")),
            asClaims(claims.map("id_token"))
        )
        else -> Claims()
    }

    @Suppress("UNCHECKED_CAST")
    private fun asClaims(claims: Map<String, Any>?) = claims
        ?.mapValues {
            val claim = it.value as Map<String, Any>
            Claim(
                claim.boolean("essential") ?: false,
                claim.string("value"),
                claim.strings("values")
            )
        }

    private fun toAudience(audience: Any?): List<String> = when (audience) {
        is List<*> -> audience.map { it.toString() }
        is String -> listOf(audience)
        else -> emptyList()
    }

    private val moshi = OAuthMoshi

    private fun parseJsonFromJWT(value: String) = try {
        val jwtParts = value.split(".")
        when {
            jwtParts.size != 3 -> Failure(InvalidRequestObject)
            else -> Success(moshi.asA<Map<String, Any>>(String(Base64.getUrlDecoder().decode(jwtParts[1]))))
        }
    } catch (e: Exception) {
        Failure(InvalidRequestObject)
    }
}
