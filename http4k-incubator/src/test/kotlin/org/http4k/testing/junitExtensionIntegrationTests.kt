package org.http4k.testing

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
    fun scenario(handler: HttpHandler) {
        assertThat(handler(Request(POST, "/foobar").body("welcome")).bodyString(), equalTo("hello"))
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
        approver: Approver
    ) {
        super.scenario(handler)
        approver.assertApproved(Response(OK).body(
            File(root, "$name.check contents are recorded as per manipulations.md").readText()
        ))
    }
}

@ExtendWith(ApprovalTest::class)
class ServirtiumReplayIntegrationTest : AContract {

    private val root = Files.createTempDirectory(".").toFile().apply { deleteOnExit() }

    init {
        File("src/test/resources/org/http4k/testing/ServirtiumReplayIntegrationTest.traffic.txt").copyTo(
            File(root, "$name.scenario.md")
        )
        File("src/test/resources/org/http4k/testing/ServirtiumReplayIntegrationTest.traffic.txt").copyTo(
            File(root, "$name.unexpected content.md")
        )
        File("src/test/resources/org/http4k/testing/ServirtiumReplayIntegrationTest.traffic.txt").copyTo(
            File(root, "$name.too many requests.md")
        )
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
        assertThat({
            handler(Request(POST, "/foobar").body("welcome"))
        }, throws(
            has(AssertionFailedError::getLocalizedMessage, containsSubstring("Unexpected request received for Interaction 1"))))
    }
}
