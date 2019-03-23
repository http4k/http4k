package org.http4k.testing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.InputStream

class Approver(private val name: String,
               private val isC: ApprovalContent,
               private val approvalSource: ApprovalSource) {

    operator fun invoke(baseMatcher: Matcher<Response> = hasStatus(OK), fn: () -> Response): Response {
        val approved = approvalSource.approvedFor(name)

        val response = fn()

        assertThat(response, baseMatcher)

        return with(approved.input()) {
            val actual = approvalSource.actualFor(name)

            when (this) {
                null -> {
                    actual.write(response)
                    throw ApprovalFailed("No approved content found", actual, approved)
                }
                else -> try {
                    assertEquals(s(this).reader().readText(), s(response).reader().readText())
                    response
                } catch (e: AssertionError) {
                    actual.write(response)
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }

    private fun ReadWriteResource.write(actual: Response) = actual.body.stream.copyTo(output())

    private fun s(inputStream: InputStream) = inputStream

    private fun s(response: Response) = response.body.stream
}

class ApprovalFailed(prefix: String, actual: ReadResource, expected: ReadResource) : RuntimeException("$prefix. To approve output:\ncp '$actual' '$expected'")
