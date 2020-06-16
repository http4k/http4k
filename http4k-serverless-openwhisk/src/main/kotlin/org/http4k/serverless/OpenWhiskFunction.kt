package org.http4k.serverless.openwhisk

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.serverless.AppLoader
import org.http4k.serverless.AppLoaderWithContexts

const val OW_REQUEST_KEY = "HTTP4K_OW_REQUEST"

open class OpenWhiskFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(object : AppLoaderWithContexts {
        override fun invoke(env: Map<String, String>, contexts: RequestContexts) = input(env)
    })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    fun main(request: JsonObject) =
        ServerFilters.InitialiseRequestContext(contexts)
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
        getAsJsonPrimitive("__ow_path").asString)
    val withQueries = getAsJsonObject("__ow_query").entrySet().fold(raw) { acc, next ->
        acc.query(next.key, next.value.asJsonPrimitive.asString)
    }
    return getAsJsonObject("__ow_headers").entrySet().fold(withQueries) { acc, next ->
        acc.header(next.key, next.value.asJsonPrimitive.asString)
    }
}
