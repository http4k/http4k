package org.http4k.bridge

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponseFactory.INSTANCE
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Head
import io.micronaut.http.annotation.Options
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Trace
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

interface MicronautToHttp4kFallbackController {
    val http4k: HttpHandler

    @Delete("/")
    fun delete(request: HttpRequest<InputStream>) = request.handle()

    @Delete("/{+path}")
    fun delete(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Get("/")
    fun get(request: HttpRequest<InputStream>) = request.handle()

    @Get("/{path}")
    fun get(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Patch("/")
    fun patch(request: HttpRequest<InputStream>) = request.handle()

    @Patch("/{+path}")
    fun patch(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Post("/")
    fun post(request: HttpRequest<InputStream>) = request.handle()

    @Post("/{+path}")
    fun post(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Put("/")
    fun put(request: HttpRequest<InputStream>) = request.handle()

    @Put("/{+path}")
    fun put(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Options("/")
    fun options(request: HttpRequest<InputStream>) = request.handle()

    @Options("/{path}")
    fun options(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Head("/")
    fun head(request: HttpRequest<InputStream>) = request.handle()

    @Head("/{+path}")
    fun head(path: String, request: HttpRequest<InputStream>) = request.handle()

    @Trace("/")
    fun trace(request: HttpRequest<InputStream>) = request.handle()

    @Trace("/{+path}")
    fun trace(path: String, request: HttpRequest<InputStream>) = request.handle()

    private fun HttpRequest<InputStream>.handle() = this@MicronautToHttp4kFallbackController.http4k(asHttp4k()).fromHttp4k()
}

fun HttpRequest<InputStream>.asHttp4k() = headers
    .toList()
    .fold(Request(Method.valueOf(methodName), uri.toString())) { acc, next ->
        next.value.fold(acc) { acc2, value -> acc2.header(next.key, value) }
    }
    .body(body.getOrNull() ?: "".byteInputStream())

fun Response.fromHttp4k(): HttpResponse<InputStream> =
    INSTANCE
        .status<InputStream>(status.code, status.description)
        .body(body.stream)
        .apply { this@fromHttp4k.headers.forEach { header(it.first, it.second) } }
