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
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.http4k.traffic.replayingMatchingContent
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.AssertionFailedError

/**
 * JUnit 5 extension for recording HTTP traffic to disk in Servirtium format.
 */
class ServirtiumRecording(
    private val baseName: String,
    private val httpHandler: HttpHandler,
    private val storageProvider: StorageProvider,
    private val interactionOptions: InteractionOptions = Defaults) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isRecordingControl()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any =
        with(ec.testInstance.get()) {
            val testName = "$baseName.${ec.requiredTestMethod.name}"

            val storage = storageProvider(testName).apply { clean() }
            if (pc.isHttpHandler())
                RecordTo(Sink.Servirtium(storage, interactionOptions))
                    .then(httpHandler)
            else InteractionControl.StorageBased(storage)
        }
}

/**
 * JUnit 5 extension for replaying HTTP traffic from disk in Servirtium format.
 */
class ServirtiumReplay(private val baseName: String,
                       private val storageProvider: StorageProvider,
                       private val options: InteractionOptions = Defaults) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isRecordingControl()

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

private fun ParameterContext.isRecordingControl() =
    parameter.parameterizedType.typeName == InteractionControl::class.java.name

private fun ParameterContext.isHttpHandler() =
    parameter.parameterizedType.typeName == "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

