package org.http4k.traffic

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.util.concurrent.atomic.AtomicInteger

fun Replay.replayingMatchingContent(): HttpHandler {
    val interactions = requests().zip(responses()).iterator()
    val count = AtomicInteger()

    return { received: Request ->
        val index = count.getAndIncrement()

        when {
            interactions.hasNext() -> {
                val (expectedReq, response) = interactions.next()

                val actual = received.removeHeadersNotIn(expectedReq).toString()
                if (expectedReq.toString() == actual) response
                else renderMismatch(index, expectedReq.toString(), actual)
            }
            else -> renderMismatch(index, "", received.toString())
        }

    }
}

private fun renderMismatch(index: Int, expectedReq: String, actual: String) = Response(Status.NOT_IMPLEMENTED).body(
    "Unexpected request received for Interaction $index ==> " +
        "expected:<$expectedReq> but was:<$actual>")

private fun Request.removeHeadersNotIn(checkReq: Request) =
    headers.fold(this) { acc, nextExpectedHeader ->
        if (checkReq.header(nextExpectedHeader.first) != null) acc
        else acc.removeHeader(nextExpectedHeader.first)
    }
