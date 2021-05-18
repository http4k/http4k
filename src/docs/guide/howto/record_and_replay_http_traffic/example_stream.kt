package guide.howto.record_and_replay_http_traffic

import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Responder

fun main() {

    // set up storage to stash a stream of HTTP traffic. Disk and Memory implementations are provided.
    val storage = ReadWriteStream.Memory()

    // wrap any HTTP Handler in a Recording Filter and play traffic through it
    val recording = TrafficFilters.RecordTo(storage).then { Response(OK).body("hello world") }
    recording(Request(GET, "http://localhost:8000/"))

    // now set up a responder
    val handler = Responder.from(storage)

    // the responder will replay the responses in order
    println(handler(Request(GET, "http://localhost:8000/")))

    // we can also replay a series of requests through a real HTTP client
    val client = ApacheClient()
    storage.requests().forEach { println(client(it)) }
}
