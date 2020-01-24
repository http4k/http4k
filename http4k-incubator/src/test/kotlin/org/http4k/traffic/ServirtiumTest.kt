package org.http4k.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

@ExtendWith(ApprovalTest::class)
class ServirtiumTest {

    @Test
    fun `sink stores traffic in servirtium markdown format, applying manipulations to recording only`(approver: Approver) {
        val received = AtomicReference<ByteArray>()
        val sink = Sink.Servirtium(Consumer(received::set), Filter { next ->
            {
                next(
                    it.removeHeader("toBeRemoved").body(it.bodyString() + it.bodyString())
                ).run { removeHeader("toBeRemoved").body(bodyString() + bodyString()) }
            }
        })

        val request1 = Request(GET, "/hello?query=123")
            .header("header1", "value1")
            .header("toBeRemoved", "notThere")
            .body("body1")

        val response1 = Response(OK)
            .header("header3", "value3")
            .header("toBeRemoved", "notThere")
            .body("body2")

        sink[request1] = response1

        approver.assertApproved(Response(OK).body(received.get().inputStream()))
    }

    @Test
    fun `replay replays traffic in servirtium markdown format`() {
        val replay = Replay.Servirtium(Supplier {
            javaClass.getResourceAsStream("/org/http4k/traffic/storedTraffic.txt").readAllBytes()
        })

        val request1 = Request(GET, "/hello?query=123")
            .header("header1", "value1")
            .body("body")

        val response1 = Response(OK)
            .header("header3", "value3")
            .body("body1")

        assertThat(replay.requests().toList(), equalTo(listOf(request1)))
        assertThat(replay.responses().toList(), equalTo(listOf(response1)))
    }

    @Test
    fun `replay replays traffic in servirtium markdown format, applying manipulations to recording`() {
        val content = javaClass.getResourceAsStream("/org/http4k/traffic/storedTraffic.txt").readAllBytes()

        val replay = Replay.Servirtium(
            Supplier { content }
        ) {
            it.header("toBeAdded", "value").body(it.bodyString() + it.bodyString())
        }

        val request1 = Request(GET, "/hello?query=123")
            .header("header1", "value1")
            .body("body")

        val response1 = Response(OK)
            .header("header3", "value3")
            .header("toBeAdded", "value")
            .body("body1body1")

        assertThat(replay.requests().toList(), equalTo(listOf(request1)))
        assertThat(replay.responses().toList(), equalTo(listOf(response1)))
    }
}
