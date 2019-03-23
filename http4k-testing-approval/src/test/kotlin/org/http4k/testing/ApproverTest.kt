package org.http4k.testing

import com.natpryce.hamkrest.Matcher
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

class ApproverTest {

    private val baseFile = Files.createTempDirectory(javaClass.name).toFile()
    private val approvalSource = FileSystemApprovalSource(baseFile)
    private val testName = "somename" + Random.nextLong()
    private val actualFile = File(baseFile, "$testName.actual")
    private val approvedFile = File(baseFile, "$testName.approved")

    @Test
    fun `throws when base matcher doesn't match and actual file not created`() {
        assertThat({ approverExpecting(hasStatus(I_M_A_TEAPOT)) }, throws<AssertionError>())
        assertThat(actualFile.exists(), equalTo(false))
    }

    @Test
    fun `when no approval recorded, create actual and throw`() {
        assertThat(
            { approverExpecting(hasStatus(OK)) },
            throws<ApprovalFailed>())
        assertThat(actualFile.exists(), equalTo(true))
        assertThat(actualFile.readText(), equalTo("content"))
        assertThat(approvedFile.exists(), equalTo(false))
    }

    @Test
    fun `when mismatch, overwrite actual`() {
        approvedFile.writeText("some other value")
        actualFile.writeText("previous content")
        assertThat(
            { approverExpecting(hasStatus(OK)) },
            throws<AssertionError>())
        assertThat(approvedFile.readText(), equalTo("some other value"))
        assertThat(actualFile.readText(), equalTo("content"))
    }

    @Test
    fun `when match, don't write actual and return response`() {
        approvedFile.writeText("content")
        assertThat(approverExpecting(hasStatus(OK)), equalTo(Response(OK).body("content")))
        assertThat(actualFile.exists(), equalTo(false))
        assertThat(approvedFile.readText(), equalTo("content"))
    }

    private fun approverExpecting(baseMatcher: Matcher<Response>) =
        Approver(testName, approvalSource)(baseMatcher) { Response(OK).body("content") }
}