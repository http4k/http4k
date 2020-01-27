package org.http4k.servirtium

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingServerTest : TestContract {

    override val uri get() = Uri.of("http://localhost:${control.port()}")

    private val storage = InteractionStorageLookup.InMemory()

    private val app = { _: Request -> Response(OK).body("hello") }.asServer(SunHttp(0))

    override lateinit var control: ServirtiumServer

    @BeforeEach
    fun start(info: TestInfo) {
        val appPort = app.start().port()
        control = ServirtiumServer.Recording(
            info.displayName,
            Uri.of("http://localhost:$appPort"),
            storage,
            object : InteractionOptions {
                override fun modify(request: Request) = request.removeHeader("Host").removeHeader("User-agent")
                override fun modify(response: Response) = response.removeHeader("Date")
            })


            control.start()
    }

    @AfterEach
    fun stop() {
        app.stop()
        control.stop()
    }

    @Test
    fun `check contents are recorded as per manipulations`(info: TestInfo, approver: Approver) {
        super.scenario()
        approver.assertApproved(Response(OK).body(String(storage(info.displayName).get())))
    }
}
