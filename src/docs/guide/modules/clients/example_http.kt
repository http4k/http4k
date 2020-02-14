package guide.modules.clients

import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.client.ApacheAsyncClient
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import kotlin.concurrent.thread

fun main() {

    // standard client
    val client = ApacheClient()
    val request = Request(GET, "http://httpbin.org/get").query("location", "John Doe")
    val response = client(request)
    println("SYNC")
    println(response.status)
    println(response.bodyString())

    // streaming client
    val streamingClient = ApacheClient(responseBodyMode = BodyMode.Stream)
    val streamingRequest = Request(GET, "http://httpbin.org/stream/100")
    println("STREAM")
    println(streamingClient(streamingRequest).bodyString())

    // async supporting clients can be passed a callback...
    val asyncClient = ApacheAsyncClient()
    asyncClient(Request(GET, "http://httpbin.org/stream/5")) {
        println("ASYNC")
        println(it.status)
        println(it.bodyString())
    }

    // ... but must be closed
    thread {
        Thread.sleep(500)
        asyncClient.close()
    }

    // custom configured client
    val customClient = ApacheClient(
        client =
        HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build()).build()
    )
}

