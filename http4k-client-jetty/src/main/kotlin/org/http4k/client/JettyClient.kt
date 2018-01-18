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
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.eclipse.jetty.client.api.Request as JettyRequest
import org.eclipse.jetty.client.api.Response as JettyResponse

class JettyClient(private val client: HttpClient = defaultHttpClient(),
                  private val bodyMode: BodyMode = BodyMode.Memory,
                  private val requestModifier: (JettyRequest) -> JettyRequest = { it }) : HttpHandler, AsyncHttpClient {
    init {
        client.start()
    }

    override fun invoke(request: Request): Response = client.send(request)

    override fun invoke(request: Request, fn: (Response) -> Unit) = client.sendAsync(request, fn)

    private fun HttpClient.send(request: Request): Response {
        return with(newRequest(request)) {
            try {
                when (bodyMode) {
                    BodyMode.Memory -> send().let { it.toHttp4kResponse().body(it.contentAsString) }
                    BodyMode.Stream -> InputStreamResponseListener().run {
                        send(this)
                        get(timeoutOrMax(), TimeUnit.MILLISECONDS).toHttp4kResponse().body(inputStream)
                    }
                }
            } catch (e: TimeoutException) {
                Response(Status.CLIENT_TIMEOUT.describeClientError(e))
            }
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
            newRequest(request.uri.toString()).method(request.method.name), { memo, (key, value) ->
        memo.header(key, value)
    }).content(InputStreamContentProvider(request.body.stream)).let(requestModifier)

    private fun JettyRequest.timeoutOrMax() = if (timeout <= 0) Long.MAX_VALUE else timeout

    private fun JettyResponse.toHttp4kResponse(): Response =
            Response(Status(status, reason)).headers(headers.toHttp4kHeaders())

    private fun HttpFields.toHttp4kHeaders(): Headers = flatMap { it.values.map { hValue -> it.name to hValue } }

    private fun Throwable.asHttp4kResponse(): Response = Response(when (this) {
        is TimeoutException -> Status.CLIENT_TIMEOUT
        else -> Status.SERVICE_UNAVAILABLE
    }.describeClientError(this as Exception))

    companion object {
        private fun defaultHttpClient() = HttpClient().apply {
            isFollowRedirects = false
            cookieStore = HttpCookieStore.Empty()
        }
    }
}