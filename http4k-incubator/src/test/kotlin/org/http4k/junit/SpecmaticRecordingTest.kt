package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import `in`.specmatic.core.NamedStub
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asFormatString
import org.http4k.format.Jackson.prettify
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(ApprovalTest::class)
class SpecmaticRecordingTest {

    object AContract

    @Test
    fun `records the values into the recording`(approver: Approver) {
        val stub = JUnitStub(AContract)

        val originalRequest = Request(POST, "/foo")
            .header("toBeRetained", "reqHeaderValue1")
            .header("toBeRemoved", "reqHeaderValue2")
            .body("helloWorldRequest")

        val originalResponse = Response(INTERNAL_SERVER_ERROR)
            .header("toBeRetained", "respHeaderValue1")
            .header("toBeRemoved", "respHeaderValue2")
            .body("helloWorldResponse")

        val httpHandler = { it: Request ->
            assertThat(it, equalTo(originalRequest))
            originalResponse
        }

        val stubs = AtomicReference<List<NamedStub>>()

        val specmaticRecording = SpecmaticRecording(stubs::set, httpHandler)

        specmaticRecording.beforeTestExecution(stub)

        @Suppress("UNCHECKED_CAST")
        val actualResponse = specmaticRecording.resolveParameter(stub, stub)(originalRequest)

        assertThat(actualResponse, equalTo(originalResponse))

        specmaticRecording.afterTestExecution(stub)

        approver.assertApproved(Response(OK).body(prettify(asFormatString(stubs.get()))))
    }
}
