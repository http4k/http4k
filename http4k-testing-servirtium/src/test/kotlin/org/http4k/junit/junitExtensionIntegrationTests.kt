package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.InteractionControl
import org.http4k.servirtium.InteractionOptions
import org.http4k.servirtium.InteractionStorage.Companion.InMemory
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.opentest4j.AssertionFailedError
import java.io.File

interface TestContract {

    @Test
    @JvmDefault
    fun scenario(handler: HttpHandler, control: InteractionControl) {
        control.addNote("this is a note")

        assertThat(handler(Request(POST, "/foobar").body("welcome")).bodyString(), equalTo("hello"))

        control.addNote("this is another note")

        assertThat(handler(Request(POST, "/foobar").body("welcome")).bodyString(), equalTo("hello"))

        control.addNote("this is yet another note")
    }
}

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingIntegrationTest : TestContract {

    private val storage = InMemory()

    private val originalHandler: HttpHandler = { Response(OK).body("hello") }

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording(
        "contractName",
        originalHandler,
        storage,
        object : InteractionOptions {
            override fun modify(request: Request) = request.body(request.bodyString() + request.bodyString())
            override fun modify(response: Response) = response.body(response.bodyString() + "2")
        }
    )

    @Test
    fun `check contents are recorded as per manipulations`(
        handler: HttpHandler,
        control: InteractionControl,
        approver: Approver
    ) {
        super.scenario(handler, control)
        approver.assertApproved(Response(OK).body(
            String(storage("contractName.check contents are recorded as per manipulations").get())
        ))
    }

    @AfterEach
    fun `check that raw handler is passed into non test method`(handler: HttpHandler) {
        assertTrue(originalHandler === handler)
    }
}

class ServirtiumReplayIntegrationTest : TestContract {

    private val storage = InMemory()

    init {
        File("src/test/resources/org/http4k/junit/ServirtiumReplayIntegrationTest.check contents are recorded as per manipulations.approved").also {
            storage("contractName.scenario").accept(it.readText().toByteArray())
            storage("contractName.unexpected content").accept(it.readText().toByteArray())
            storage("contractName.too many requests").accept(it.readText().toByteArray())
        }
    }

    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay("contractName", storage,
        object : InteractionOptions {
            override fun modify(request: Request) = request.body(request.bodyString().replace("2", ""))
        })

    @Test
    fun `unexpected content`(handler: HttpHandler) {
        assertThat({
            handler(Request(GET, "/foobar").body("welcome"))
        }, throws(
            has(AssertionFailedError::getLocalizedMessage, containsSubstring("Unexpected request received for Interaction 0"))))
    }

    @Test
    fun `too many requests`(handler: HttpHandler) {
        handler(Request(POST, "/foobar").body("welcome"))
        handler(Request(POST, "/foobar").body("welcome"))
        assertThat({
            handler(Request(POST, "/foobar").body("welcome"))
        }, throws(
            has(AssertionFailedError::getLocalizedMessage, containsSubstring("Unexpected request received for Interaction 2"))))
    }
}
