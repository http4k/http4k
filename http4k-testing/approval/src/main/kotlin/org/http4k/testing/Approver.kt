package org.http4k.testing

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.opentest4j.AssertionFailedError
import java.io.InputStream

/**
 * Coordinates the comparison of the content for a test.
 */
interface Approver {

    /**
     * Check the content of the passed message against the previously approved content.
     */
    fun <T : HttpMessage> assertApproved(httpMessage: T)
}

class NamedResourceApprover(private val name: String,
                            private val approvalContent: ApprovalContent,
                            private val approvalSource: ApprovalSource) : Approver {

    override fun <T : HttpMessage> assertApproved(httpMessage: T) {
        val approved = approvalSource.approvedFor(name)

        with(approved.input()) {
            val actual = approvalSource.actualFor(name)

            when (this) {
                null -> with(approvalContent(httpMessage)) {
                    if (available() > 0) {
                        copyTo(actual.output())
                        throw ApprovalFailed("No approved content found", actual, approved)
                    }
                }
                else -> try {
                    assertEquals(approvalContent(this).reader().use { it.readText() }, approvalContent(httpMessage).reader().readText())
                } catch (e: AssertionError) {
                    approvalContent(httpMessage).copyTo(actual.output())
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }
}

class ApprovalFailed(prefix: String, actual: ReadResource, expected: ReadResource) : RuntimeException("$prefix. To approve output:\nmv '$actual' '$expected'")

fun Approver.assertApproved(response: Response, expectedStatus: Status) = assertApproved(response.apply { assertEquals(expectedStatus, response.status) })
fun Approver.assertApproved(content: String) = assertApproved(Response(Status.OK).body(content))
fun Approver.assertApproved(content: InputStream) = assertApproved(Response(Status.OK).body(content))

/**
 * Create a Hamkrest Matcher for this message that can be combined with other Matchers
 */
fun <T : HttpMessage> Approver.hasApprovedContent(): Matcher<T> = object : Matcher<T> {

    override val description = "has previously approved content"

    override fun invoke(actual: T): MatchResult =
        try {
            assertApproved(actual)
            Match
        } catch (e: AssertionFailedError) {
            Mismatch(e.localizedMessage)
        }
}
