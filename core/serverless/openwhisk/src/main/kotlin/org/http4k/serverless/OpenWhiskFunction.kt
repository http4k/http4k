package org.http4k.serverless

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.http4k.base64DecodedByteBuffer
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.lens.RequestKey
import org.http4k.serverless.DetectBinaryBody.Companion.NonBinary
import java.util.Locale.getDefault

val OW_REQUEST_KEY = RequestKey.required<JsonObject>("HTTP4K_OW_REQUEST")

class OpenWhiskFunction(
    appLoader: AppLoader,
    private val detectBinaryBody: DetectBinaryBody = NonBinary
) : (JsonObject) -> JsonObject {

    private val app = runBlocking { appLoader(System.getenv()) }

    override fun invoke(request: JsonObject) =
        runBlocking {
            CatchAll()
                .then(AddOpenWhiskRequest(request))
                .then(app)
                .invoke(request.asHttp4k()).toGson()

        }

    private fun Response.toGson() = JsonObject().apply {
        addProperty("statusCode", status.code)
        addProperty(
            "body",
            if (detectBinaryBody.isBinary(this@toGson)) body.payload.base64Encode() else bodyString()
        )
        add("headers", JsonObject().apply {
            headers.forEach {
                addProperty(it.first, it.second)
            }
        })
    }

    private fun JsonObject.asHttp4k(): Request {
        val baseRequest = Request(
            Method.valueOf(getAsJsonPrimitive("__ow_method").asString.uppercase(getDefault())),
            stringOrEmpty("__ow_path") + if (has("__ow_query")) "?" + get("__ow_query").asJsonPrimitive.asString else ""
        ).body(stringOrEmpty("__ow_body"))

        val withQueries = getQueries().fold(baseRequest) { acc, next ->
            acc.query(next.key, next.value.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asString ?: "")
        }

        val fullRequest = mapFrom("__ow_headers").fold(withQueries) { acc, next ->
            acc.header(next.key, next.value.asJsonPrimitive.asString)
        }

        return if (detectBinaryBody.isBinary(fullRequest)) fullRequest.body(
            Body(fullRequest.body.payload.base64DecodedByteBuffer())
        )
        else fullRequest
    }
}

private fun AddOpenWhiskRequest(request: JsonObject) = Filter { next ->
    {
        next(it.with(OW_REQUEST_KEY of request))
    }
}

private fun JsonObject.getQueries() = entrySet().filterNot { it.key.startsWith("__ow_") }

private fun JsonObject.mapFrom(key: String) =
    get(key)?.takeIf { get(key).isJsonObject }?.asJsonObject?.entrySet() ?: emptySet()

private fun JsonObject.stringOrEmpty(key: String) = get(key)?.takeIf { get(key).isJsonPrimitive }?.asString ?: ""
