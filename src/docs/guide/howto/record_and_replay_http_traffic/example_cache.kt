package guide.howto.record_and_replay_http_traffic

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteCache

fun main() {

    // set up storage to cache a set of HTTP traffic. Disk and Memory implementations are provided.
    val storage = ReadWriteCache.Disk()

    // wrap any HTTP Handler in a Recording Filter and play traffic through it
    val withCachedContent =
        TrafficFilters.ServeCachedFrom(storage)
            .then(TrafficFilters.RecordTo(storage))
            .then {
                Response(OK).body("hello world")
            }
    val aRequest = Request(GET, "http://localhost:8000/")
    println(withCachedContent(aRequest))

    // repeated requests are intercepted by the cache and the responses provided without hitting the original handler
    println(withCachedContent(Request(GET, "http://localhost:8000/")))
}
