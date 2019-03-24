package org.http4k.testing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Coordinates the comparison of the content for a test.
 */
interface Approver {
    operator fun <T : HttpMessage> invoke(fn: () -> T): T
}

class NamedResourceApprover(private val name: String,
                            private val approvalContent: ApprovalContent,
                            private val approvalSource: ApprovalSource) : Approver {

    override operator fun <T : HttpMessage> invoke(fn: () -> T): T {
        val approved = approvalSource.approvedFor(name)

        val message = fn()

        return with(approved.input()) {
            val actual = approvalSource.actualFor(name)

            when (this) {
                null -> {
                    approvalContent(message).copyTo(actual.output())
                    throw ApprovalFailed("No approved content found", actual, approved)
                }
                else -> try {
                    assertEquals(approvalContent(this).reader().readText(), approvalContent(message).reader().readText())
                    message
                } catch (e: AssertionError) {
                    approvalContent(message).copyTo(actual.output())
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }
}

class ApprovalFailed(prefix: String, actual: ReadResource, expected: ReadResource) : RuntimeException("$prefix. To approve output:\ncp '$actual' '$expected'")

operator fun <T : HttpMessage> Approver.invoke(baseMatcher: Matcher<T>, fn: () -> T): T = invoke {
    fn().apply { assertThat(this, baseMatcher) }
}

operator fun Approver.invoke(status: Status, fn: () -> Response): Response = invoke {
    fn().apply { assertThat(this.status, equalTo(status)) }
}
