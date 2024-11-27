package org.http4k.connect.ollama

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun Ollama.Companion.Http(http: HttpHandler = JavaHttpClient()) = object : Ollama {

    private val routedHttp = SetBaseUriFrom(Uri.of("http://localhost:11434"))
        .then(http)

    override fun <R> invoke(action: OllamaAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
