package org.http4k.junit

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.then
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.servirtium.ServirtiumContract
import org.http4k.traffic.ByteStorage.Companion.Disk
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.http4k.traffic.replayingMatchingContent
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.opentest4j.AssertionFailedError
import java.io.File

/**
 * JUnit 5 extension for recording HTTP traffic to disk in Servirtium format.
 */
class ServirtiumRecording(private val httpHandler: HttpHandler,
                          private val root: File = File("."),
                          private val requestManipulations: (Request) -> Request = { it },
                          private val responseManipulations: (Response) -> Response = { it }) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supportedParam()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract ->
                    RecordTo(Sink.Servirtium(Disk(File(root, "$name.${ec.requiredTestMethod.name}.md"), true),
                        requestManipulations, responseManipulations))
                        .then(httpHandler)
                else -> throw IllegalArgumentException("Class is not an instance of: ${ServirtiumContract::name}")
            }
        }
}

/**
 * JUnit 5 extension for replaying HTTP traffic from disk in Servirtium format.
 */
class ServirtiumReplay(private val root: File = File("."),
                       private val requestManipulations: (Request) -> Request = { it }) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supportedParam()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): HttpHandler =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract ->
                    ConvertBadResponseToAssertionFailed().then(
                        Replay.Servirtium(
                            Disk(File(root, "$name.${ec.requiredTestMethod.name}.md"), true))
                            .replayingMatchingContent(requestManipulations)
                    )
                else -> throw IllegalArgumentException("Class is not an instance of: ${ServirtiumContract::name}")
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

private fun ParameterContext.supportedParam() = parameter.parameterizedType.typeName ==
    "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"
