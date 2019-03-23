package org.http4k.testing

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

/**
 * JUnit extension for injecting an Approver into a JUnit5 test-case
 */
class ApprovalTest : BeforeTestExecutionCallback, ParameterResolver {
    private val STORE_KEY = "approver"

    override fun beforeTestExecution(context: ExtensionContext) =
        store(context).put(STORE_KEY, Approver(
            TestNamer.Simple.nameFor(context.requiredTestClass, context.requiredTestMethod),
            ApprovalContent.BodyOnly(),
            FileSystemApprovalSource(File("src/test/resources"))
        ))

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        parameterContext.parameter.type == Approver::class.java

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        if (supportsParameter(parameterContext, context)) store(context)[STORE_KEY] else null

    private fun store(context: ExtensionContext) = with(context) {
        getStore(create(requiredTestClass.name, requiredTestMethod.name))
    }
}