package org.http4k.testing

import org.jsoup.Jsoup
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * Approval JUnit5 extension configured to compare HTTP messages containing
 * HTML5 bodies, which are approved after being prettified
 */
open class Html5MessageApprovalTest(
    private val testNamer: TestNamer = TestNamer.ClassAndMethod,
    private val approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext): Approver =
        NamedResourceApprover(
            testNamer.nameFor(context.requiredTestClass, context.requiredTestMethod),
            ApprovalContent.HttpTextMessage { Jsoup.parse(it).toString() },
            approvalSource
        )
}
