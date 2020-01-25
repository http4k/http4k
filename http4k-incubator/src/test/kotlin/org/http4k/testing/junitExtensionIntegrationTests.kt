package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
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
        testInfo: TestInfo,
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
    }

    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay(root) {
        it.body(it.bodyString().replace("2", ""))
    }
}
