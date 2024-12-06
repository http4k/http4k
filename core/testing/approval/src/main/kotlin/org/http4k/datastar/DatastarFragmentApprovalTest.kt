package org.http4k.datastar

import com.github.underscore.Xml.formatXml
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.datastarFragments
import org.http4k.testing.ApprovalSource
import org.http4k.testing.ContentTypeAwareApprovalTest
import org.http4k.testing.FileSystemApprovalSource
import org.http4k.testing.TestNamer
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * Approval JUnit5 extension configured to compare prettified-HTML Datastar Fragment messages.
 */
class DatastarFragmentApprovalTest(
    testNamer: TestNamer = TestNamer.ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(ContentType.TEXT_EVENT_STREAM, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        val fragments = Body.datastarFragments().toLens()(Response(OK).body(input))
            .flatMap { it.fragments.map { it.value } }
            .joinToString("\n\n")

        formatXml("<span>$fragments</span>").removePrefix("<span>").removeSuffix("</span>")
    } catch (e: IllegalArgumentException) {
        throw AssertionFailedError("Invalid HTML generated", "<valid HTML>", input, e)
    }
}
