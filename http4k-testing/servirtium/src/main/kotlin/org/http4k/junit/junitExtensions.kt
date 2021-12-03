package org.http4k.junit

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.then
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.servirtium.InteractionControl
import org.http4k.servirtium.InteractionControl.Companion.NoOp
import org.http4k.servirtium.InteractionOptions
import org.http4k.servirtium.InteractionOptions.Companion.Defaults
import org.http4k.servirtium.StorageProvider
import org.http4k.servirtium.trafficPrinter
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.http4k.traffic.replayingMatchingContent
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.AssertionFailedError

/**
 * JUnit 5 extension for recording HTTP traffic to disk in Servirtium format.
 */
class ServirtiumRecording
    @JvmOverloads constructor(
        private val baseName: String,
        private val httpHandler: HttpHandler,
        private val storageProvider: StorageProvider,
        private val options: InteractionOptions = Defaults
    ) : ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isInteractionControl()

    private var inTest = false

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any = with(ec.testInstance.get()) {
        val testName = "$baseName.${ec.requiredTestMethod.name}"
        val storage = storageProvider(testName).apply { clean() }
        if (pc.isHttpHandler()) {
            when {
                inTest -> RecordTo(Sink.Servirtium(storage, options))
                    .then(options.trafficPrinter())
                    .then(httpHandler)
                else -> httpHandler
            }
        } else InteractionControl.StorageBased(storage)
    }

    override fun beforeTestExecution(context: ExtensionContext?) {
        inTest = true
    }

    override fun afterTestExecution(context: ExtensionContext) {
        inTest = false
    }
}

/**
 * JUnit 5 extension for replaying HTTP traffic from disk in Servirtium format.
 */
class ServirtiumReplay @JvmOverloads constructor(private val baseName: String,
                       private val storageProvider: StorageProvider,
                       private val options: InteractionOptions = Defaults) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isInteractionControl()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any =
        if (pc.isHttpHandler()) {
            ConvertBadResponseToAssertionFailed()
                .then(Replay.Servirtium(storageProvider("$baseName.${ec.requiredTestMethod.name}"), options)
                    .replayingMatchingContent(options::modify)
                )
        } else NoOp
}

private fun ConvertBadResponseToAssertionFailed() = Filter { next ->
    {
        with(next(it)) {
            if (status == NOT_IMPLEMENTED) throw AssertionFailedError(bodyString())
            this
        }
    }
}

private fun ParameterContext.isInteractionControl() =
    parameter.parameterizedType.typeName == InteractionControl::class.java.name

private fun ParameterContext.isHttpHandler() =
    parameter.parameterizedType.typeName == "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"
