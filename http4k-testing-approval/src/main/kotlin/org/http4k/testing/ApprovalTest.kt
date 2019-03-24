package org.http4k.testing

import org.http4k.format.Argo
import org.http4k.testing.ApprovalContent.Companion.HttpBodyOnly
import org.http4k.testing.TestNamer.Companion.Simple
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

/**
 * Base JUnit extension for injecting an Approver into a JUnit5 test-case. Implement this
 * to provide custom approval behaviours, or
 */
interface BaseApprovalTest : BeforeTestExecutionCallback, ParameterResolver {
    val namer: TestNamer
    val approvalContent: ApprovalContent
    val approvalSource: ApprovalSource

    private val STORE_KEY get() = "approver"

    override fun beforeTestExecution(context: ExtensionContext) {
        store(context).put(STORE_KEY, Approver(
            namer.nameFor(context.requiredTestClass, context.requiredTestMethod),
            approvalContent,
            approvalSource
        ))
    }

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        parameterContext.parameter.type == Approver::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        if (supportsParameter(parameterContext, context)) store(context)[STORE_KEY] else null

    private fun store(context: ExtensionContext) = with(context) {
        getStore(create(requiredTestClass.name, requiredTestMethod.name))
    }
}

/**
 * Standard Approval JUnit5 extension. Can be used to compare any HttpMessages.
 */
class ApprovalTest : BaseApprovalTest {
    override val namer = Simple
    override val approvalContent = HttpBodyOnly()
    override val approvalSource = FileSystemApprovalSource(File("src/test/resources"))
}

/**
 * Approval JUnit5 extension configured to compare prettified-JSON content.
 */
class JsonApprovalTest : BaseApprovalTest {
    override val namer = Simple
    override val approvalContent = HttpBodyOnly(Argo::prettify)
    override val approvalSource = FileSystemApprovalSource(File("src/test/resources"))
}