package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.core.JsonParseException
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.core.Uri
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidRequestObject
import org.http4k.security.openid.Nonce
import java.util.Base64

object RequestObjectExtractor {

    private val json = RequestObjectExtractorJson

    internal fun extractRequestJwtClaimsAsMap(value: String): Result<Map<*, *>, InvalidRequestObject> =
        parseJsonFromJWT(value)

    internal fun extractRequestObjectFromJwt(value: String): Result<RequestObject, InvalidRequestObject> =
        parseJsonFromJWT(value)
            .map { jsonFromJWT ->
                RequestObject(
                    client = jsonFromJWT["client_id"]?.let { ClientId(it.toString()) },
                    redirectUri = jsonFromJWT["redirect_uri"]?.let { Uri.of(it.toString()) },
                    audience = toAudience(jsonFromJWT["aud"]),
                    issuer = jsonFromJWT["iss"]?.toString(),
                    scope = jsonFromJWT["scope"]?.toString()?.split(" ") ?: emptyList(),
                    responseMode = jsonFromJWT["response_mode"]?.let { ResponseMode.fromQueryParameterValue(it.toString()) },
                    responseType = jsonFromJWT["response_type"]?.let { ResponseType.fromQueryParameterValue(it.toString()) },
                    state = jsonFromJWT["state"]?.let { State(it.toString()) },
                    nonce = jsonFromJWT["nonce"]?.let { Nonce(it.toString()) },
                    magAge = jsonFromJWT["max_age"]?.toString()?.toLong(),
                    expiry = jsonFromJWT["exp"]?.toString()?.toLong(),
                    claims = toClaims(jsonFromJWT["claims"])
                )
            }

    private fun toClaims(claims: Any?) = when (claims) {
        is Map<*, *> -> json.asA(json.asFormatString(claims))
        else -> Claims()
    }

    private fun toAudience(audience: Any?): List<String> = when (audience) {
        is List<*> -> audience.map { it.toString() }
        is String -> listOf(audience)
        else -> emptyList()
    }

    private fun parseJsonFromJWT(value: String) = try {
        val jwtParts = value.split(".")
        when {
            jwtParts.size != 3 -> Failure(InvalidRequestObject)
            else -> Success(json.asA<Map<String, Any>>(String(Base64.getUrlDecoder().decode(jwtParts[1]))))
        }
    } catch (e: IllegalArgumentException) {
        Failure(InvalidRequestObject)
    } catch (e: JsonParseException) {
        Failure(InvalidRequestObject)
    }
}

