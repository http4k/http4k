package org.http4k.junit

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.then
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.servirtium.InteractionStorageLookup
import org.http4k.servirtium.InteractionStorageLookup.Companion.Disk
import org.http4k.servirtium.RecordingControl
import org.http4k.servirtium.RecordingControl.Companion.NoOp
import org.http4k.servirtium.ServirtiumContract
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
class ServirtiumRecording(private val httpHandler: HttpHandler,
                          private val storageLookup: InteractionStorageLookup = Disk(),
                          private val requestManipulations: (Request) -> Request = { it },
                          private val responseManipulations: (Response) -> Response = { it }) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isRecordingControl()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract -> {
                    val testName = "$name.${ec.requiredTestMethod.name}"
                    storageLookup.clean(testName)

                    val storage = storageLookup(testName)
                    if (pc.isHttpHandler())
                        RecordTo(Sink.Servirtium(storage, requestManipulations, responseManipulations))
                            .then(httpHandler)
                    else RecordingControl.ByteStorage(storage)
                }
                else -> throw IllegalArgumentException("Class is not an instance of: ServirtiumContract")
            }
        }
}

/**
 * JUnit 5 extension for replaying HTTP traffic from disk in Servirtium format.
 */
class ServirtiumReplay(private val storageLookup: InteractionStorageLookup = Disk(),
                       private val requestManipulations: (Request) -> Request = { it }) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.isHttpHandler() || pc.isRecordingControl()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): Any =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract ->
                    if (pc.isHttpHandler()) {
                        ConvertBadResponseToAssertionFailed()
                            .then(Replay.Servirtium(storageLookup("$name.${ec.requiredTestMethod.name}"))
                                .replayingMatchingContent(requestManipulations)
                            )
                    } else NoOp
                else -> throw IllegalArgumentException("Class is not an instance of: ServirtiumContract")
            }
        }

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
    parameter.parameterizedType.typeName == RecordingControl::class.java.name

private fun ParameterContext.isHttpHandler() =
    parameter.parameterizedType.typeName == "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

