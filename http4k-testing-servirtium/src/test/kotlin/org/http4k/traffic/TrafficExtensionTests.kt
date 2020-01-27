package org.http4k.traffic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_PDF
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.servirtium.InteractionOptions
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

@ExtendWith(ApprovalTest::class)
class TrafficExtensionTests {

    @Test
    fun `sink stores traffic in servirtium markdown format, applying manipulations to recording only`(approver: Approver) {
        val received = AtomicReference<ByteArray>()
        val sink = Sink.Servirtium(Consumer(received::set),
            object : InteractionOptions {
                override fun requestManipulations(request: Request) =
                    request.removeHeader("toBeRemoved").body(request.bodyString() + request.bodyString())

                override fun responseManipulations(response: Response) =
                    response.removeHeader("toBeRemoved").body(response.bodyString() + response.bodyString())
            }
        )

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
    fun `sink stores binary artifacts as base64 encoded`(approver: Approver) {
        val received = AtomicReference<ByteArray>()
        val sink = Sink.Servirtium(Consumer(received::set),
            object : InteractionOptions {
                override fun isBinary(contentType: ContentType) = true
            }
        )

        sink[Request(GET, "/").body("body1").with(CONTENT_TYPE of APPLICATION_PDF)] = Response(OK).body("body2").with(CONTENT_TYPE of APPLICATION_PDF)

        approver.assertApproved(Response(OK).body(received.get().inputStream()))
    }

    @Test
    fun `replay replays traffic from servirtium markdown format`() {
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
    fun `replay replays binary traffic from servirtium markdown format`() {
        val replay = Replay.Servirtium(Supplier {
            javaClass.getResourceAsStream("/org/http4k/traffic/storedBinaryTraffic.txt").readAllBytes()
        },
            object : InteractionOptions {
                override fun isBinary(contentType: ContentType) = true
            }
        )

        val request1 = Request(GET, "/")
            .with(CONTENT_TYPE of APPLICATION_PDF)
            .body("body1")

        val response1 = Response(OK)
            .with(CONTENT_TYPE of APPLICATION_PDF)
            .body("body2")

        assertThat(replay.requests().toList(), equalTo(listOf(request1)))
        assertThat(replay.responses().toList(), equalTo(listOf(response1)))
    }
}
