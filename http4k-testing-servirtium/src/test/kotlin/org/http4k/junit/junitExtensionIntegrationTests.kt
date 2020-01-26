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
import org.http4k.servirtium.RecordingControl
import org.http4k.servirtium.ServirtiumContract
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.opentest4j.AssertionFailedError
import java.io.File
import java.nio.file.Files

interface AContract : ServirtiumContract {
    override val name get() = "contractName"

    @Test
    @JvmDefault
    fun scenario(handler: HttpHandler, control: RecordingControl) {
        control.addNote("this is a note")

        assertThat(handler(Request(POST, "/foobar").body("welcome")).bodyString(), equalTo("hello"))

        control.addNote("this is another note")

        assertThat(handler(Request(POST, "/foobar").body("welcome")).bodyString(), equalTo("hello"))

        control.addNote("this is yet another note")
    }
}

@ExtendWith(ApprovalTest::class)
class ServirtiumRecordingIntegrationTest : AContract {

    private val root = Files.createTempDirectory(".").toFile().apply { deleteOnExit() }

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording(
        { Response(OK).body("hello") },
        root,
        { it.body(it.bodyString() + it.bodyString()) },
        { it.body(it.bodyString() + "2") }
    )

    @Test
    fun `check contents are recorded as per manipulations`(
        handler: HttpHandler,
        control: RecordingControl,
        approver: Approver
    ) {
        super.scenario(handler, control)
        approver.assertApproved(Response(OK).body(
            File(root, "$name.check contents are recorded as per manipulations.md").readText()
        ))
    }
}

class ServirtiumReplayIntegrationTest : AContract {

    private val root = Files.createTempDirectory(".").toFile().apply { deleteOnExit() }

    init {
        File("src/test/resources/org/http4k/junit/ServirtiumReplayIntegrationTest.check contents are recorded as per manipulations.approved").also {
            it.copyTo(File(root, "$name.scenario.md"))
            it.copyTo(File(root, "$name.unexpected content.md"))
            it.copyTo(File(root, "$name.too many requests.md"))
        }
    }

    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay(root) {
        it.body(it.bodyString().replace("2", ""))
    }

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
