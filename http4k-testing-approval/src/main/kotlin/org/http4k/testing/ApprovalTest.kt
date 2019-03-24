package org.http4k.testing

import argo.saj.InvalidSyntaxException
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.format.Argo
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.ApprovalContent.Companion.HttpBodyOnly
import org.http4k.testing.TestNamer.Companion.ClassAndMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * Base JUnit extension for injecting an Approver into a JUnit5 test-case. Implement this
 * to provide custom approval behaviours, or
 */
interface BaseApprovalTest : BeforeTestExecutionCallback, ParameterResolver {

    fun approverFor(context: ExtensionContext): Approver

    override fun beforeTestExecution(context: ExtensionContext) = store(context).put("approver", approverFor(context))

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        parameterContext.parameter.type == Approver::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        if (supportsParameter(parameterContext, context)) store(context)["approver"] else null

    private fun store(context: ExtensionContext) = with(context) {
        getStore(create(requiredTestClass.name, requiredTestMethod.name))
    }
}

/**
 * Standard Approval JUnit5 extension. Can be used to compare any HttpMessages.
 */
class ApprovalTest : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext): Approver = NamedResourceApprover(
        ClassAndMethod.nameFor(context.requiredTestClass, context.requiredTestMethod),
        HttpBodyOnly(),
        FileSystemApprovalSource(File("src/test/resources"))
    )
}

/**
 * Approval JUnit5 extension configured to compare prettified-JSON messages.
 */
class JsonApprovalTest : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext) = object : Approver {
        private val delegate = NamedResourceApprover(
            ClassAndMethod.nameFor(context.requiredTestClass, context.requiredTestMethod),
            HttpBodyOnly {
                try {
                    Argo.prettify(it)
                } catch (e: InvalidSyntaxException) {
                    throw AssertionFailedError("Invalid JSON generated", "<valid JSON>", it)
                }
            },
            FileSystemApprovalSource(File("src/test/resources"))
        )

        override fun <T : HttpMessage> invoke(fn: () -> T) =
            delegate {
                fn().apply { assertEquals(APPLICATION_JSON, CONTENT_TYPE(this)) }
            }
    }
}