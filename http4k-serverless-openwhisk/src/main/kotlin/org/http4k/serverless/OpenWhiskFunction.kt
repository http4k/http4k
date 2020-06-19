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

const val OW_REQUEST_KEY = "HTTP4K_OW_REQUEST"

class OpenWhiskFunction(appLoader: AppLoaderWithContexts, env: Map<String, String> = System.getenv()) : (JsonObject) -> JsonObject {
    constructor(input: AppLoader) : this(object : AppLoaderWithContexts {
        override fun invoke(env: Map<String, String>, contexts: RequestContexts) = input(env)
    })

    private val contexts = RequestContexts()
    private val app = appLoader(env, contexts)

    override fun invoke(request: JsonObject) =
        InitialiseRequestContext(contexts)
            .then(AddOpenWhiskRequest(request, contexts))
            .then(app)
            .invoke(request.asHttp4k()).toGson()
}

private fun AddOpenWhiskRequest(request: JsonElement, contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][OW_REQUEST_KEY] = request
        next(it)
    }
}

private fun Response.toGson() = JsonObject().apply {
    addProperty("code", status.code)
    addProperty("body", bodyString())
    add("headers", JsonObject().apply {
        headers.forEach {
            addProperty(it.first, it.second)
        }
    })
}

private fun JsonObject.asHttp4k(): Request {
    val raw = Request(
        Method.valueOf(getAsJsonPrimitive("__ow_method").asString.toUpperCase()),
        stringOrEmpty("__ow_path"))
        .body(Body(stringOrEmpty("__ow_body")))

    val withQueries = mapFrom("__ow_query").fold(raw) { acc, next ->
        acc.query(next.key, next.value.asJsonPrimitive.asString)
    }
    return mapFrom("__ow_headers").fold(withQueries) { acc, next ->
        acc.header(next.key, next.value.asJsonPrimitive.asString)
    }
}

private fun JsonObject.mapFrom(key: String) =
    get(key)?.takeIf { get(key).isJsonObject }?.asJsonObject?.entrySet() ?: emptySet()

private fun JsonObject.stringOrEmpty(key: String) = get(key)?.takeIf { get(key).isJsonPrimitive }?.asString ?: ""
