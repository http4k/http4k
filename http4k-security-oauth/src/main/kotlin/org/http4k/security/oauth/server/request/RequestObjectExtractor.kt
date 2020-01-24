package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.map
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
import java.util.Base64
import kotlin.reflect.KClass

object RequestObjectExtractor {

    internal fun extractRequestJwtClaimsAsMap(value: String): Result<Map<*, *>, InvalidRequestObject> {
        return parseJsonFromJWT(value, Map::class)
    }

    internal fun extractRequestObjectFromJwt(value: String): Result<RequestObject, InvalidRequestObject> {
        return parseJsonFromJWT(value, RequestObject::class)
    }

    private fun <T : Any> parseJsonFromJWT(value: String, target: KClass<T>): Result<T, InvalidRequestObject> {
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

}
