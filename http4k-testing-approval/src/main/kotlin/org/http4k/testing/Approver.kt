package org.http4k.testing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Assertions.assertEquals

class Approver(private val name: String,
               private val approvalSource: ApprovalSource) {

    operator fun invoke(baseMatcher: Matcher<Response> = hasStatus(OK), fn: () -> Response): Response {
        val approved = approvalSource.approvedFor(name)

        val response = fn()

        assertThat(response, baseMatcher)

        return with(approved.input()) {
            val actual = approvalSource.actualFor(name)

            when (this) {
                null -> {
                    actual.writeActual(response)
                    throw ApprovalFailed("No approved content found", actual, approved)
                }
                else -> try {
                    assertEquals(reader().readText(), response.bodyString())
                    response
                } catch (e: AssertionError) {
                    actual.writeActual(response)
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }

    private fun ReadWriteResource.writeActual(actual: Response) =
        with(output().writer()) {
            use { write(actual.bodyString()) }
        }
}

private class ApprovalFailed(prefix: String, actual: ReadResource, expected: ReadResource) : RuntimeException("$prefix. To approve output:\ncp '$actual' '$expected'")
