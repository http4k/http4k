package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.ServirtiumContract
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Optional

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingTest {

    class Stub(private val t: Any) : ExtensionContext by proxy(), ParameterContext by proxy() {
        override fun getTestInstance() = Optional.of(t)
        override fun getTestMethod() = Optional.of(ServirtiumRecordingTest::class.java.getMethod("hashCode"))
    }

    object AContract : ServirtiumContract {
        override val name get() = "name"
    }

    @TempDir
    lateinit var root: File

    @Test
    fun `records the values into the recording`(approver: Approver) {
        val requestManipulations = { it: Request -> it.removeHeader("toBeRemoved") }
        val responseManipulations = { it: Response ->
            it.removeHeader("toBeRemoved")
                .body(it.bodyString().replace("hello", "goodbye"))
        }

        val stub = Stub(AContract)

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

        val actualResponse = ServirtiumRecording(httpHandler, root, requestManipulations, responseManipulations).resolveParameter(stub, stub)(originalRequest)

        assertThat(actualResponse, equalTo(originalResponse))

        val expectedFile = File(root, "name.hashCode.md")

        approver.assertApproved(Response(OK).body(expectedFile.readText()))
    }
}
