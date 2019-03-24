package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

class NamedSourceApproverTest {

    private val body = "content"
    private val baseFile = Files.createTempDirectory(javaClass.name).toFile()
    private val testName = "somename" + Random.nextLong()
    private val actualFile = File(baseFile, "$testName.actual")
    private val approvedFile = File(baseFile, "$testName.approved")
    private val approver = NamedResourceApprover(testName, ApprovalContent.HttpBodyOnly(), FileSystemApprovalSource(baseFile))

    @Test
    fun `when no approval recorded, create actual and throw`() {
        assertThat({ approver { Response(OK).body(body) } }, throws<ApprovalFailed>())
        assertThat(actualFile.exists(), equalTo(true))
        assertThat(actualFile.readText(), equalTo(body))
        assertThat(approvedFile.exists(), equalTo(false))
    }

    @Test
    fun `when mismatch, overwrite actual`() {
        approvedFile.writeText("some other value")
        actualFile.writeText("previous content")
        assertThat({ approver { Response(OK).body(body) } }, throws<AssertionError>())
        assertThat(actualFile.readText(), equalTo(body))
        assertThat(approvedFile.readText(), equalTo("some other value"))
    }

    @Test
    fun `when match, don't write actual and return response`() {
        approvedFile.writeText(body)
        assertThat(approver { Response(OK).body(body) }, equalTo(Response(OK).body(body)))
        assertThat(actualFile.exists(), equalTo(false))
        assertThat(approvedFile.readText(), equalTo(body))
    }

    @Test
    fun `when base matcher fails, don't even create actual`() {
        assertThat({ approver(hasStatus(I_M_A_TEAPOT)) { Response(OK).body(body) } }, throws<AssertionError>())
        assertThat(actualFile.exists(), equalTo(false))
        assertThat(approvedFile.exists(), equalTo(false))
    }

}