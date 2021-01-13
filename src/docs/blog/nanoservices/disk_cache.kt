package blog.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.filter.TrafficFilters.ServeCachedFrom
import org.http4k.server.Http4kServer
import org.http4k.server.KtorCIO
import org.http4k.server.asServer
import org.http4k.traffic.ReadWriteCache
import java.io.File

fun `disk cache!`(dir: String): Http4kServer {
    val cache = ReadWriteCache.Disk(dir)
    return ProxyHost(Https)
        .then(RecordTo(cache))
        .then(ServeCachedFrom(cache))
        .then(ReportHttpTransaction { println(it.request.uri) })
        .then(JavaHttpClient())
        .asServer(KtorCIO())
        .start()
}

fun main() {
    System.setProperty("http.proxyHost", "localhost")
    System.setProperty("http.proxyPort", "8000")
    System.setProperty("http.nonProxyHosts", "localhost")

    val client = JavaHttpClient()
    val dir = "store"
    File(dir).deleteRecursively()

    `disk cache!`(dir).use {
        val request = Request(GET, "http://api.github.com/users/http4k")

        println(client(request).bodyString())

        // this request is served from the cache, so will not generate a call
        println(client(request).bodyString())
    }
}
