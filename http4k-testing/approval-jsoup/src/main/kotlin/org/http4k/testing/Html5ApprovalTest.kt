package org.http4k.testing

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.testing.ApprovalContent.Companion.HttpTextMessage
import org.http4k.testing.TestNamer.Companion.ClassAndMethod
import org.jsoup.Jsoup
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

private fun formatWithJSoup(input: String) =
    Jsoup.parse(input).toString()

/**
 * Approval JUnit5 extension configured to compare prettified HTML5 bodies.
 */
open class Html5ApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(TEXT_HTML, testNamer, approvalSource) {
    override fun format(input: String): String = formatWithJSoup(input)
}

/**
 * Approval JUnit5 extension configured to compare HTTP messages containing
 * HTML5 bodies, which are approved after being prettified
 */
open class Html5MessageApprovalTest(
    private val testNamer: TestNamer = ClassAndMethod,
    private val approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext): Approver =
        NamedResourceApprover(
            testNamer.nameFor(context.requiredTestClass, context.requiredTestMethod),
            HttpTextMessage(::formatWithJSoup),
            approvalSource
        )
}
