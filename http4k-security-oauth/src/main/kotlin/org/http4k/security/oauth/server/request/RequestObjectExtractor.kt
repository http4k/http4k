package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Uri
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.lens.BiDiMapping
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidRequestObject
import org.http4k.security.openid.Nonce
import org.http4k.util.Failure
import org.http4k.util.Result
import org.http4k.util.Success
import org.http4k.util.map
import java.util.Base64
import kotlin.reflect.KClass

object RequestObjectExtractor {

    internal fun extractRequestJwtClaimsAsMap(value: String): Result<InvalidRequestObject, Map<*, *>> {
        return parseJsonFromJWT(value, Map::class)
    }

    internal fun extractRequestObjectFromJwt(value: String): Result<InvalidRequestObject, RequestObject> {
        return parseJsonFromJWT(value, RequestObjectJson::class)
            .map { jsonFromJWT ->
                RequestObject(
                    client = jsonFromJWT.client,
                    redirectUri = jsonFromJWT.redirectUri,
                    audience = toAudience(jsonFromJWT.audience),
                    issuer = jsonFromJWT.issuer,
                    scope = jsonFromJWT.scope?.split(" ") ?: emptyList(),
                    responseMode = jsonFromJWT.responseMode,
                    responseType = jsonFromJWT.responseType,
                    state = jsonFromJWT.state,
                    nonce = jsonFromJWT.nonce,
                    magAge = jsonFromJWT.magAge,
                    expiry = jsonFromJWT.expiry,
                    claims = jsonFromJWT.claims
                )
            }
    }

    private fun toAudience(jsonValue: JsonNode): List<String> {
        return when (jsonValue) {
            is ArrayNode -> jsonValue.map { it.textValue() }
            is TextNode -> listOf(jsonValue.textValue())
            else -> emptyList()
        }
    }

    private fun <T : Any> parseJsonFromJWT(value: String, target: KClass<T>): Result<InvalidRequestObject, T> {
        try {
            val jwtParts = value.split(".")
            if (jwtParts.size != 3) {
                return Failure(InvalidRequestObject)
            }
            return Success(RequestObjectExtractorJson.asA(String(Base64.getUrlDecoder().decode(jwtParts[1])), target))
        } catch (e: IllegalArgumentException) {
            return Failure(InvalidRequestObject)
        } catch (e: JsonParseException) {
            return Failure(InvalidRequestObject)
        }
    }

    internal object RequestObjectExtractorJson : ConfigurableJackson(
        KotlinModule()
            .asConfigurable()
            .text(BiDiMapping(ResponseMode::class.java, { ResponseMode.fromQueryParameterValue(it) }, { it.queryParameterValue }))
            .text(BiDiMapping(ResponseType::class.java, { ResponseType.fromQueryParameterValue(it) }, { it.queryParameterValue }))
            .text(BiDiMapping(ClientId::class.java, { ClientId(it) }, { it.value }))
            .text(BiDiMapping(Uri::class.java, { Uri.of(it) }, { it.toString() }))
            .text(BiDiMapping(State::class.java, { State(it) }, { it.value }))
            .text(BiDiMapping(Nonce::class.java, { Nonce(it) }, { it.value }))
            .done()
            .setSerializationInclusion(NON_NULL)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(FAIL_ON_IGNORED_PROPERTIES, false)
            .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(USE_BIG_INTEGER_FOR_INTS, true)
            .configure(FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)

    )

    internal data class RequestObjectJson(@JsonProperty("client_id") val client: ClientId? = null,
                                          @JsonProperty("redirect_uri") val redirectUri: Uri? = null,
                                          @JsonProperty("aud") val audience: JsonNode,
                                          @JsonProperty("iss") val issuer: String? = null,
                                          @JsonProperty("scope") val scope: String?,
                                          @JsonProperty("response_mode") val responseMode: ResponseMode? = null,
                                          @JsonProperty("response_type") val responseType: ResponseType? = null,
                                          @JsonProperty("state") val state: State? = null,
                                          @JsonProperty("nonce") val nonce: Nonce? = null,
                                          @JsonProperty("max_age") val magAge: Long? = null,
                                          @JsonProperty("exp") val expiry: Long? = null,
                                          @JsonProperty("claims") val claims: Claims = Claims())

}
