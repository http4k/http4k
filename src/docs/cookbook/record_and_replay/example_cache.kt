package cookbook.record_and_replay

import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteCache

fun main() {

    // set up storage to cache a set of HTTP traffic. Disk and Memory implementations are provided.
    val storage = ReadWriteCache.Disk()

    // wrap any HTTP Handler in a Recording Filter and play traffic through it
    val withCachedContent = TrafficFilters.ServeCachedFrom(storage).then { Response(Status.OK).body("hello world") }
    val aRequest = Request(GET, "http://localhost:8000/")
    withCachedContent(aRequest)

    // repeated requests are intercepted by the cache and the responses provided without hitting the original handler
    println(withCachedContent(Request(GET, "http://localhost:8000/")))
}
