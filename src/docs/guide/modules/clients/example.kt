package guide.modules.clients

import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request

fun main(args: Array<String>) {

    // standard client
    val client = ApacheClient()
    val request = Request(Method.GET, "http://httpbin.org/get").query("location", "John Doe")
    val response = client(request)
    println(response.status)
    println(response.bodyString())

    // streaming client
    val streamingClient = ApacheClient(responseBodyMode = BodyMode.Stream)
    val streamingRequest = Request(Method.GET, "http://httpbin.org/stream/100")
    println(streamingClient(streamingRequest).bodyString())

    // custom configured client
    val customClient = ApacheClient(
        client =
        HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .build()).build()
    )
}

