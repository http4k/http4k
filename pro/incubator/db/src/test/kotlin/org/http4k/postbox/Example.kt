package org.http4k.postbox

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.db.InMemoryTransactor
import org.http4k.db.Transactor
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.util.UUID
import java.util.concurrent.Executors


val statusUriTemplate = UriTemplate.from("/api/status/{requestId}")

fun main() {
    ThirdPartySlowService().asServer(SunHttp(8000)).start()

    val transactor = InMemoryTransactor<Postbox>(InMemoryPostbox()).also {
        // please notice in-memory transactor locks the postbox so multiple threads can't access it at the same time
        startOutboxBackgroundProcessor(it)
    }

    val transactionalOutbox = PostboxInterceptorHandler(transactor, requestIdResolver(), statusUriTemplate)

    routes(
        "/api/reverse/{message}" bind POST to MyServiceHandler(transactionalOutbox),
        PostboxStatusHandler(transactor, statusUriTemplate)
    ).asServer(SunHttp(9000)).start()
}

fun ThirdPartySlowService() = { req: Request -> Thread.sleep(10000); Response(OK).body(req.bodyString().reversed()) }

fun startOutboxBackgroundProcessor(transactor: Transactor<Postbox>) {
    Executors.newVirtualThreadPerTaskExecutor().execute {
        while (true) {
            ProcessPendingRequests(transactor, JavaHttpClient())
            Thread.sleep(5000)
        }
    }
}

private fun requestIdResolver() = { req: Request ->
    RequestId.of(req.header("x-idempotency-key") ?: UUID.randomUUID().toString())
}

private fun MyServiceHandler(client: HttpHandler) = { request: Request ->
    val message = request.path("message")!!

    // Makes the request to the third party service.
    // If the client is a transactional outbox, rather than the real thing:
        // On first call it'll store the request and return a 202 with a Link header to check the status of the request.
        // On subsequent calls it'll return either 202 again or the response from the third party service.
    client(Request(POST, "http://localhost:8000")
        .header("x-idempotency-key", request.header("x-idempotency-key"))
        .body(message))
}
