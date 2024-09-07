package org.http4k.client

import org.eclipse.jetty.client.BufferingResponseListener
import org.eclipse.jetty.client.ByteBufferRequestContent
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.InputStreamRequestContent
import org.eclipse.jetty.client.InputStreamResponseListener
import org.eclipse.jetty.client.Result
import org.eclipse.jetty.http.HttpCookieStore
import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpFields
import org.http4k.asByteBuffer
import org.http4k.client.PreCannedJettyHttpClients.defaultJettyHttpClient
import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import org.http4k.core.toParametersMap
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import org.eclipse.jetty.client.Request as JettyRequest
import org.eclipse.jetty.client.Response as JettyResponse

object JettyClient {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        client: HttpClient = defaultJettyHttpClient(),
        bodyMode: BodyMode = BodyMode.Memory,
        requestModifier: (JettyRequest) -> JettyRequest = { it }
    ): DualSyncAsyncHttpHandler {
        if (!client.isRunning) client.start()

        return object : DualSyncAsyncHttpHandler {
            override fun close() = client.stop()

            override fun invoke(request: Request): Response = client.send(request)

            override fun invoke(request: Request, fn: (Response) -> Unit) = client.sendAsync(request, fn)

            private fun HttpClient.send(request: Request): Response = with(newRequest(request)) {
                try {
                    when (bodyMode) {
                        BodyMode.Memory -> send().let { it.toHttp4kResponse().body(Body(it.content.asByteBuffer())) }
                        BodyMode.Stream -> InputStreamResponseListener().run {
                            send(this)
                            get(timeoutOrMax(), MILLISECONDS).toHttp4kResponse().body(inputStream)
                        }
                    }
                } catch (e: ExecutionException) {
                    when (e.cause) {
                        is UnknownHostException -> Response(UNKNOWN_HOST.toClientStatus(e))
                        is ConnectException -> Response(CONNECTION_REFUSED.toClientStatus(e))
                        else -> throw e
                    }
                } catch (e: TimeoutException) {
                    Response(CLIENT_TIMEOUT.toClientStatus(e))
                }
            }

            private fun HttpClient.sendAsync(request: Request, fn: (Response) -> Unit) {
                with(newRequest(request)) {
                    when (bodyMode) {
                        BodyMode.Memory -> send(object : BufferingResponseListener() {
                            override fun onComplete(result: Result) {
                                val response = if (result.isFailed) {
                                    result.failure.asHttp4kResponse()
                                } else {
                                    result.response.toHttp4kResponse().body(contentAsString)
                                }
                                fn(response)
                            }
                        })

                        BodyMode.Stream -> send(object : InputStreamResponseListener() {
                            override fun onHeaders(response: JettyResponse) {
                                super.onHeaders(response)
                                executor.execute {
                                    fn(response.toHttp4kResponse().body(inputStream))
                                }
                            }

                            override fun onFailure(response: JettyResponse, failure: Throwable) {
                                super.onFailure(response, failure)
                                fn(failure.asHttp4kResponse())
                            }
                        })
                    }
                }
            }

            private fun HttpClient.newRequest(request: Request): org.eclipse.jetty.client.Request =
                newRequest(request.uri.toString()).method(request.method.name)
                    .headers { fields -> request.headers.toParametersMap().forEach { fields.put(it.key, it.value) } }
                    .body(
                        when (bodyMode) {
                            BodyMode.Memory -> ByteBufferRequestContent(request.body.payload)
                            BodyMode.Stream -> InputStreamRequestContent(request.body.stream)
                        }
                    )
                    .let(requestModifier)
                    .let { jettyRequest ->
                        request.body.length?.let { len ->
                            jettyRequest.headers { headers -> headers.add("content-length", len.toString()) }
                        } ?: jettyRequest
                    }


            private fun JettyRequest.timeoutOrMax() = if (timeout <= 0) Long.MAX_VALUE else timeout

            private fun JettyResponse.toHttp4kResponse(): Response =
                Response(Status(status, reason)).headers(headers.toHttp4kHeaders())

            private fun HttpFields.toHttp4kHeaders(): Headers =
                flatMap { it.values.map { hValue -> it.name to hValue } }

            private fun Throwable.asHttp4kResponse(): Response = Response(
                when (this) {
                    is TimeoutException -> CLIENT_TIMEOUT
                    else -> SERVICE_UNAVAILABLE
                }.description("Client Error: caused by $localizedMessage")
            )
        }
    }
}

object PreCannedJettyHttpClients {
    fun defaultJettyHttpClient() = HttpClient().apply {
        isFollowRedirects = false
        httpCookieStore = HttpCookieStore.Empty()
    }
}
