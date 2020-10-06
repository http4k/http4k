package org.http4k.client

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.BufferingResponseListener
import org.eclipse.jetty.client.util.InputStreamContentProvider
import org.eclipse.jetty.client.util.InputStreamResponseListener
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.util.HttpCookieStore
import org.http4k.core.BodyMode
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import org.eclipse.jetty.client.api.Request as JettyRequest
import org.eclipse.jetty.client.api.Response as JettyResponse

object JettyClient {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        client: HttpClient = defaultHttpClient(),
        bodyMode: BodyMode = BodyMode.Memory,
        requestModifier: (JettyRequest) -> JettyRequest = { it }
    ): DualSyncAsyncHttpHandler {
        if (!client.isStarted && !client.isStarting) client.start()

        return object : DualSyncAsyncHttpHandler {
            override fun close() = client.stop()

            override fun invoke(request: Request): Response = client.send(request)

            override fun invoke(request: Request, fn: (Response) -> Unit) = client.sendAsync(request, fn)

            private fun HttpClient.send(request: Request): Response = with(newRequest(request)) {
                try {
                    when (bodyMode) {
                        BodyMode.Memory -> send().let { it.toHttp4kResponse().body(it.contentAsString) }
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

            private fun HttpClient.newRequest(request: Request): JettyRequest = request.headers.fold(
                newRequest(request.uri.toString()).method(request.method.name)) { memo, (key, value) ->
                memo.header(key, value)
            }.content(InputStreamContentProvider(request.body.stream)).let(requestModifier)

            private fun JettyRequest.timeoutOrMax() = if (timeout <= 0) Long.MAX_VALUE else timeout

            private fun JettyResponse.toHttp4kResponse(): Response =
                Response(Status(status, reason)).headers(headers.toHttp4kHeaders())

            private fun HttpFields.toHttp4kHeaders(): Headers = flatMap { it.values.map { hValue -> it.name to hValue } }

            private fun Throwable.asHttp4kResponse(): Response = Response(when (this) {
                is TimeoutException -> CLIENT_TIMEOUT
                else -> SERVICE_UNAVAILABLE
            }.description("Client Error: caused by $localizedMessage"))

        }
    }

    private fun defaultHttpClient() = HttpClient().apply {
        isFollowRedirects = false
        cookieStore = HttpCookieStore.Empty()
    }
}
