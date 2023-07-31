package org.http4k.client

import io.helidon.common.http.Http
import io.helidon.nima.webclient.ClientResponse
import io.helidon.nima.webclient.RuntimeUnknownHostException
import io.helidon.nima.webclient.WebClient
import io.helidon.nima.webclient.http1.Http1Client
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.io.UncheckedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException

object HelidonClient {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        client: Http1Client = WebClient.builder().followRedirect(false).build(),
        bodyMode: BodyMode = Memory,
    ): HttpHandler = object : HttpHandler {

        override fun invoke(request: Request) = try {
            client.makeHelidonRequest(request).asHttp4k()
        } catch (e: UncheckedIOException) {
            when (e.cause) {
                is ConnectException -> Response(UNKNOWN_HOST.toClientStatus(e))
                is SocketTimeoutException -> Response(CLIENT_TIMEOUT.toClientStatus(e))
                else -> throw e
            }
        } catch (e: RuntimeUnknownHostException) {
            Response(UNKNOWN_HOST.toClientStatus(e))
        }

        private fun Http1Client.makeHelidonRequest(request: Request) = request.headers.groupBy { it.first }
            .entries
            .fold(method(Http.Method.create(request.method.name)).uri(request.uri.toString())) { acc, next ->
                acc.header(Http.Header.create(Http.Header.createName(next.key, next.key), next.value.map { it.second }))
            }.submit(request.body.payload.array())

        private fun ClientResponse.asHttp4k() =
            headers().fold(Response(Status(status().code(), status().reasonPhrase()))) { acc, next ->
                next.allValues().fold(acc) { acc2, value -> acc2.header(next.name(), value) }
            }.body(bodyMode(inputStream()))
    }
}
