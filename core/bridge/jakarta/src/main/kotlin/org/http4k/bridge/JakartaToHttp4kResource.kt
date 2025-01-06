package org.http4k.bridge

import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HEAD
import jakarta.ws.rs.OPTIONS
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Request
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import java.io.InputStream

@Path("/{.*}")
abstract class JakartaToHttp4kResource {

    @Inject
    lateinit var http: HttpHandler

    @GET
    fun get(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @POST
    fun post(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @PUT
    fun put(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @DELETE
    fun delete(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @PATCH
    fun patch(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @OPTIONS
    fun options(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    @HEAD
    fun head(@Context req: Request, @Context uri: UriInfo, @Context headers: HttpHeaders, input: InputStream) =
        req.handle(headers, input, uri)

    private fun Request.handle(headers: HttpHeaders, input: InputStream, uri: UriInfo) =
        http(toHttp4k(headers, uri, input)).fromHttp4k()
}

private fun Request.toHttp4k(
    headers: HttpHeaders,
    uri: UriInfo,
    input: InputStream
) = headers.requestHeaders.toList()
    .fold(org.http4k.core.Request(Method.valueOf(method), uri.requestUri.toString())) { acc, (key, value) ->
        value.fold(acc) { acc2, header -> acc2.header(key, header) }
    }
    .body(input)

private fun org.http4k.core.Response.fromHttp4k() =
    Response.status(status.code, status.description).entity(body.stream)
        .apply { headers.forEach { (key, value) -> header(key, value) } }
        .build()
