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
import java.io.File
import java.nio.file.Files

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingServerTest : TestContract {

    override val uri get() = Uri.of("http://localhost:${mitm.port()}")

    private val root = Files.createTempDirectory(".").toFile().apply { deleteOnExit() }
    private val app = { _: Request -> Response(OK).body("hello") }.asServer(SunHttp(0))
    private lateinit var mitm: ServirtumRecordingServer

    override val control by lazy { mitm }

    @BeforeEach
    fun start(info: TestInfo) {
        val appPort = app.start().port()
        mitm = ServirtumRecordingServer(
            info.displayName,
            Uri.of("http://localhost:$appPort"),
            requestManipulations = { it.removeHeader("Host").removeHeader("User-agent") },
            responseManipulations = { it.removeHeader("Date") },
            root = root
        )
        mitm.start()
    }

    @AfterEach
    fun stop() {
        app.stop()
        mitm.stop()
    }

    @Test
    fun `check contents are recorded as per manipulations`(info: TestInfo, approver: Approver) {
        super.scenario()
        approver.assertApproved(Response(OK).body(
            File(root, "${info.displayName}.md").readText()
        ))
    }
}
