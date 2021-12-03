package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.InteractionOptions
import org.http4k.servirtium.InteractionStorage
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingTest {

    object AContract

    private val storage = InteractionStorage.InMemory()

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

        val servirtiumRecording = ServirtiumRecording(
            "name",
            httpHandler,
            storage,
            object : InteractionOptions {
                override fun modify(request: Request) = request.removeHeader("toBeRemoved")
                override fun modify(response: Response) = response.removeHeader("toBeRemoved")
                    .body(response.bodyString().replace("hello", "goodbye"))
            }
        )

        servirtiumRecording.beforeTestExecution(stub)

        @Suppress("UNCHECKED_CAST")
        val actualResponse = (servirtiumRecording.resolveParameter(stub, stub) as HttpHandler)(originalRequest)

        assertThat(actualResponse, equalTo(originalResponse))

        approver.assertApproved(Response(OK).body(String(storage("name.hashCode").get())))
    }
}
