package org.http4k.client

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.ResponseResultOf
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.toParametersMap
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration

private typealias FuelRequest = com.github.kittinunf.fuel.core.Request
private typealias FuelResponse = com.github.kittinunf.fuel.core.Response
private typealias FuelResult = com.github.kittinunf.result.Result<ByteArray, FuelError>
private typealias FuelFuel = com.github.kittinunf.fuel.Fuel

class Fuel(
    private val bodyMode: BodyMode = Memory,
    private val timeout: Duration = Duration.ofSeconds(15)
) :
    DualSyncAsyncHttpHandler {

    override fun invoke(request: Request): Response = request.toFuel().response().toHttp4k()

    override fun invoke(request: Request, fn: (Response) -> Unit) {
        request.toFuel().response { fuelRequest: FuelRequest, response: FuelResponse, result: FuelResult ->
            fn(Triple(fuelRequest, response, result).toHttp4k())
        }
    }

    private fun ResponseResultOf<ByteArray>.toHttp4k(): Response {
        val (_, response, result) = this
        val (_, error) = result
        when (error?.exception) {
            is ConnectException -> return Response(Status.CONNECTION_REFUSED.toClientStatus(error.exception as ConnectException))
            is UnknownHostException -> return Response(Status.UNKNOWN_HOST.toClientStatus(error.exception as UnknownHostException))
            is SocketTimeoutException -> return Response(Status.CLIENT_TIMEOUT.toClientStatus(error.exception as SocketTimeoutException))
        }
        val headers: Parameters = response.headers.toList().fold(listOf()) { acc, next ->
            acc + next.second.fold(listOf()) { keyAcc, nextValue -> keyAcc + (next.first to nextValue) }
        }
        return Response(Status(response.statusCode, response.responseMessage))
            .headers(headers)
            .body(bodyMode(response.body().toStream()))
    }

    private fun Request.toFuel(): com.github.kittinunf.fuel.core.Request =
        FuelFuel.request(Method.valueOf(method.toString()), uri.toString(), emptyList())
            .allowRedirects(false)
            .timeout(timeout.toMillisPart())
            .timeoutRead(timeout.toMillisPart())
            .header(headers.toParametersMap())
            .also { fuelRequest ->
                when (bodyMode) {
                    Memory -> fuelRequest.body(body.payload.array())
                    Stream -> {
                        fuelRequest.body(body.stream, body.length?.let<Long, () -> Long> { { it } })
                    }
                }
            }
}
