package org.http4k.servirtium

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ServirtiumReplayServerTest : TestContract {

    override val uri get() = Uri.of("http://localhost:${control.port()}")

    private val storage = InteractionStorage.InMemory()

    override lateinit var control: ServirtiumServer

    @BeforeEach
    fun start(info: TestInfo) {
        javaClass.getResourceAsStream("/org/http4k/servirtium/ServirtiumReplayServerTest.txt").reader().use { r ->
            storage(info.displayName).accept(r.readText().toByteArray())
        }

        control = ServirtiumServer.Replay(
            info.displayName,
            storage,
            object : InteractionOptions {
                override fun modify(request: Request) = request.removeHeader("Host").removeHeader("User-agent")
            }
        )
        control.start()
    }

    @AfterEach
    fun stop() {
        control.stop()
    }

    @Test
    @Disabled
    fun `unexpected content`(approver: Approver) {
        approver.assertApproved(createHandler()(Request(GET, "/foo")), NOT_IMPLEMENTED)
    }

    @Test
    @Disabled
    fun `too many requests`(approver: Approver) {
        super.scenario()
        val httpMessage = createHandler()(Request(GET, "/foo")).run {
            body(bodyString()
                .replace(Regex("Host.*"), "Host: localhost")
                .replace(Regex("User-agent.*"), "User-agent: bob")
            )
        }

        approver.assertApproved(httpMessage, NOT_IMPLEMENTED)
    }
}
