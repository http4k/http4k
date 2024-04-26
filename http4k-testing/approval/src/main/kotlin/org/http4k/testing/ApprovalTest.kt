package org.http4k.testing

import com.github.underscore.Json
import com.github.underscore.Json.JsonStringBuilder.Step.TWO_SPACES
import com.github.underscore.Json.formatJson
import com.github.underscore.Xml.formatXml
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpMessage
import org.http4k.format.JacksonYaml
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.ApprovalContent.Companion.HttpTextBody
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
        HttpTextBody(),
        FileSystemApprovalSource(File("src/test/resources"))
    )
}

/**
 * Approval testing JUnit5 extension that checks the expected content type is present in the
 */
abstract class ContentTypeAwareApprovalTest(
    private val contentType: ContentType,
    private val testNamer: TestNamer = ClassAndMethod,
    private val approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : BaseApprovalTest {
    override fun approverFor(context: ExtensionContext) = object : Approver {
        override fun <T : HttpMessage> assertApproved(httpMessage: T) {
            delegate.assertApproved(httpMessage)
            assertEquals(contentType, CONTENT_TYPE(httpMessage))
        }

        private val delegate: Approver = NamedResourceApprover(
            testNamer.nameFor(context.requiredTestClass, context.requiredTestMethod),
            HttpTextBody(::format),
            approvalSource
        )

        override fun withNameSuffix(suffix: String): Approver = delegate.withNameSuffix(suffix)
    }

    abstract fun format(input: String): String
}

/**
 * Approval JUnit5 extension configured to compare prettified-JSON messages.
 */
class JsonApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(APPLICATION_JSON, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        formatJson(input, TWO_SPACES)
    } catch (e: Json.ParseException) {
        throw AssertionFailedError("Invalid JSON generated", "<valid JSON>", input, e)
    }
}

/**
 * Approval JUnit5 extension configured to compare prettified-JSON messages.
 */
class CloudEventsJsonApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(ContentType.CLOUD_EVENT_JSON, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        formatJson(input, TWO_SPACES)
    } catch (e: Json.ParseException) {
        throw AssertionFailedError("Invalid JSON generated", "<valid JSON>", input, e)
    }
}

/**
 * Approval JUnit5 extension configured to compare prettified-HTML messages. Note that this strips
 * <!DOCTYPE tags from the start of the document.
 */
class HtmlApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(TEXT_HTML, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        formatXml(input)
    } catch (e: IllegalArgumentException) {
        throw AssertionFailedError("Invalid HTML generated", "<valid HTML>", input, e)
    }
}

/**
 * Approval JUnit5 extension configured to compare prettified-XML messages.
 */
class XmlApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(APPLICATION_XML, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        formatXml(input)
    } catch (e: IllegalArgumentException) {
        throw AssertionFailedError("Invalid XML generated", "<valid XML>", input, e)
    }
}

/**
 * Approval JUnit5 extension configured to compare prettified-YAML messages.
 */
class YamlApprovalTest(
    testNamer: TestNamer = ClassAndMethod,
    approvalSource: ApprovalSource = FileSystemApprovalSource(File("src/test/resources"))
) : ContentTypeAwareApprovalTest(APPLICATION_YAML, testNamer, approvalSource) {
    override fun format(input: String): String = try {
        JacksonYaml.asA<Map<String, Any>>(input)
        input
    } catch (e: Exception) {
        throw AssertionFailedError("Invalid YAML generated", "<valid YAML>", input, e)
    }
}
