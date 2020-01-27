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

    override val uri get() = Uri.of("http://localhost:${control.port()}")

    private val root = Files.createTempDirectory(".").toFile().apply { deleteOnExit() }
    private val app = { _: Request -> Response(OK).body("hello") }.asServer(SunHttp(0))

    override lateinit var control: ServirtiumServer

    @BeforeEach
    fun start(info: TestInfo) {
        val appPort = app.start().port()
        control = ServirtiumServer.Recording(
            info.displayName,
            Uri.of("http://localhost:$appPort"),
            StorageFactory.Disk(root),
            requestManipulations = { it.removeHeader("Host").removeHeader("User-agent") },
            responseManipulations = { it.removeHeader("Date") }
        )
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
        approver.assertApproved(Response(OK).body(
            File(root, "${info.displayName}.md").readText()
        ))
    }
}
