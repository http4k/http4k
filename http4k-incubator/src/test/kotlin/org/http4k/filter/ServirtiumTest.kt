package org.http4k.filter

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.traffic.Sink
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.io.File

@ExtendWith(ApprovalTest::class)
class ServirtiumTest {

    @Test
    fun `sink stores in markdown format`(@TempDir tempDir: File, approver: Approver) {

        val output = File(tempDir, "output.md")

        val servirtium = Sink.Servirtium(output)

        servirtium[Request(GET, "/hello?query=123")
            .header("header1", "value1")
            .header("Content-Type", "request-content-type/value")
            .body("body1")] = Response(OK)
            .header("header3", "value3")
            .header("Content-Type", "response-content-type/value")
            .body("body1")

        servirtium[Request(POST, "/")] = Response(INTERNAL_SERVER_ERROR)

        approver.assertApproved(Response(OK).body(output.readText()))
    }

}