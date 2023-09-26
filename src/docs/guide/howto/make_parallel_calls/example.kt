package guide.howto.make_parallel_calls

import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.ServerFilters
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.http4k.util.withRequestTracing
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.system.measureTimeMillis

private fun serverWithTracing(handler: HttpHandler) = Filter.NoOp
    .then(ServerFilters.RequestTracing())
    .then(DebuggingFilters.PrintRequestAndResponse())
    .then(handler)

private fun clientWithTracing() = Filter.NoOp
    .then(ClientFilters.RequestTracing())
    .then(DebuggingFilters.PrintRequestAndResponse())
    .then(OkHttp())

private fun serverWithParallelCallsAndTracing(
    dependencyServerPort: Int,
    executor: ExecutorService
): HttpHandler {
    val localClient = clientWithTracing()

    return serverWithTracing { _: Request ->
        val overallStatus = (1..5).map {
            executor.submit(Callable {
                localClient(
                    Request(
                        GET,
                        "http://localhost:$dependencyServerPort/sub-request-${it}"
                    )
                )
            })
        }.foldRight(OK) { future, overallStatus ->
            val nextResponse = future.get(5, SECONDS)
            if (nextResponse.status.successful) nextResponse.status else overallStatus
        }

        Response(overallStatus)
    }
}

fun main() {
    val originalExecutor = ThreadPoolExecutor(5, 5, 10, SECONDS, LinkedBlockingDeque())

    // convert the execution service to one that propagates the trace information
    val executor = originalExecutor.withRequestTracing(ZipkinTracesStorage.THREAD_LOCAL)

    val subServer = serverWithTracing { _: Request -> Thread.sleep(500); Response(OK) }
    val runningSubServer = subServer.asServer(Undertow(0)).start()

    val server = serverWithParallelCallsAndTracing(runningSubServer.port(), executor)
    val runningServer = server.asServer(Undertow(0)).start()

    val elapsedTime = measureTimeMillis {
        val client = clientWithTracing()
        client(Request(GET, "http://localhost:${runningServer.port()}/main-request"))
    }

    println("Elapsed time in millis = $elapsedTime")

    executor.close()
    runningSubServer.stop()
    runningServer.stop()
}

