package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.core.JsonParseException
import com.natpryce.Failure
import org.http4k.security.oauth.server.InvalidRequestObject
import com.natpryce.Result
import com.natpryce.Success
import org.http4k.format.Jackson
import java.util.Base64

object RequestObjectExtractor {

    internal fun extractRequestJwtClaimsAsMap(value: String): Result<Map<*, *>, InvalidRequestObject> {
        try {
            val jwtParts = value.split(".")
            if (jwtParts.size != 3) {
                return Failure(InvalidRequestObject)
            }
            return Success(Jackson.asA(String(Base64.getUrlDecoder().decode(jwtParts[1])), Map::class))
        } catch (e: IllegalArgumentException) {
            return Failure(InvalidRequestObject)
        } catch (e: JsonParseException) {
            return Failure(InvalidRequestObject)
        }
    }

}
