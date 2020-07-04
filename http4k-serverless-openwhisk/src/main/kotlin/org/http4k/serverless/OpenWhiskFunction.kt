package org.http4k.serverless

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.serverless.DetectBinaryBody.Companion.NonBinary
import java.nio.ByteBuffer
import java.util.Base64.getDecoder
import java.util.Base64.getEncoder

const val OW_REQUEST_KEY = "HTTP4K_OW_REQUEST"

class OpenWhiskFunction(
    appLoader: AppLoaderWithContexts,
    env: Map<String, String> = System.getenv(),
    private val detectBinaryBody: DetectBinaryBody = NonBinary
) : (JsonObject) -> JsonObject {

    constructor(
        input: AppLoader,
        env: Map<String, String> = System.getenv(),
        detectBinaryBody: DetectBinaryBody = NonBinary
    ) : this(object : AppLoaderWithContexts {
        override fun invoke(env: Map<String, String>, contexts: RequestContexts) = input(env)
    }, env, detectBinaryBody)

    private val contexts = RequestContexts()
    private val app = appLoader(env, contexts)

    override fun invoke(request: JsonObject): JsonObject = InitialiseRequestContext(contexts)
        .then(AddOpenWhiskRequest(request, contexts))
        .then(app)
        .invoke(request.asHttp4k()).toGson()

    private fun Response.toGson() = JsonObject().apply {
        addProperty("statusCode", status.code)
        addProperty(
            "body",
            if (detectBinaryBody.isBinary(this@toGson)) getEncoder().encodeToString(body.payload.array()) else bodyString()
        )
        add("headers", JsonObject().apply {
            headers.forEach {
                addProperty(it.first, it.second)
            }
        })
    }

    private fun JsonObject.asHttp4k(): Request {
        val baseRequest = Request(
            Method.valueOf(getAsJsonPrimitive("__ow_method").asString.toUpperCase()),
            stringOrEmpty("__ow_path") + if (has("__ow_query")) "?" + get("__ow_query").asJsonPrimitive.asString else ""
        ).body(stringOrEmpty("__ow_body"))

        val withQueries = getQueries().fold(baseRequest) { acc, next ->
            acc.query(next.key, next.value.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asString ?: "")
        }

        val fullRequest = mapFrom("__ow_headers").fold(withQueries) { acc, next ->
            acc.header(next.key, next.value.asJsonPrimitive.asString)
        }

        return if (detectBinaryBody.isBinary(fullRequest)) fullRequest.body(
            Body(ByteBuffer.wrap(getDecoder().decode(fullRequest.body.payload.array())))
        )
        else fullRequest
    }
}

private fun AddOpenWhiskRequest(request: JsonElement, contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][OW_REQUEST_KEY] = request
        next(it)
    }
}

private fun JsonObject.getQueries() = entrySet().filterNot { it.key.startsWith("__ow_") }

private fun JsonObject.mapFrom(key: String) =
    get(key)?.takeIf { get(key).isJsonObject }?.asJsonObject?.entrySet() ?: emptySet()

private fun JsonObject.stringOrEmpty(key: String) = get(key)?.takeIf { get(key).isJsonPrimitive }?.asString ?: ""
