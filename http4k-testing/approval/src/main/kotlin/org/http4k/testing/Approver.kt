package org.http4k.testing

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Header
import org.junit.jupiter.api.Assertions.assertEquals
import org.opentest4j.AssertionFailedError
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Coordinates the comparison of the content for a test.
 */
interface Approver {

    /**
     * Check the content of the passed message against the previously approved content.
     */
    fun <T : HttpMessage> assertApproved(httpMessage: T)

    fun withNameSuffix(suffix: String): Approver
}

class NamedResourceApprover(
    private val name: String,
    private val approvalContent: ApprovalContent,
    private val approvalSource: ApprovalSource,
    private val transformer: ApprovalTransformer<*> = ApprovalTransformer.StringWithNormalisedLineEndings()
) : Approver {

    override fun withNameSuffix(suffix: String) = NamedResourceApprover(
        name = "$name.$suffix",
        approvalContent = approvalContent,
        approvalSource = approvalSource
    )

    override fun <T : HttpMessage> assertApproved(httpMessage: T) {
        val approved = approvalSource.approvedFor(name)
        val actual = approvalSource.actualFor(name)
        val actualBytes = approvalContent(httpMessage).readBytes()

        with(approved.input()) {
            actual.output() // ensure the actual is removed

            when (this) {
                null -> when {
                    actualBytes.isNotEmpty() -> {
                        actual.output().write(actualBytes)
                        throw ApprovalFailed("No approved content found", actual, approved)
                    }

                    else -> {}
                }

                else -> try {
                    assertEquals(transformer(approvalContent(this)), transformer(ByteArrayInputStream(actualBytes)))
                } catch (e: AssertionError) {
                    ByteArrayInputStream(actualBytes).copyTo(actual.output())
                    throw AssertionError(ApprovalFailed("Mismatch", actual, approved).message + "\n" + e.message)
                }
            }
        }
    }
}

class ApprovalFailed(prefix: String, actual: ReadResource, expected: ReadResource) :
    RuntimeException("$prefix. To approve output:\nmv '$actual' '$expected'")

fun Approver.assertApproved(response: Response, expectedStatus: Status) =
    assertApproved(response.apply { assertEquals(expectedStatus, response.status) })

fun Approver.assertApproved(content: String, contentType: ContentType? = null) =
    assertApproved(Response(Status.OK).body(content).with(Header.CONTENT_TYPE of contentType))

fun Approver.assertApproved(content: InputStream, contentType: ContentType? = null) =
    assertApproved(Response(Status.OK).body(content).with(Header.CONTENT_TYPE of contentType))

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
