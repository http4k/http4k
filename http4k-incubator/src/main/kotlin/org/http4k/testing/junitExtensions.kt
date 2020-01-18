package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Servirtium
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * JUnit 5 extension for recording HTTP traffic to disk in Servirtium format.
 */
class ServirtiumRecording(private val httpHandler: HttpHandler,
                          private val root: File = File(".")) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supportedParam()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract -> TrafficFilters.RecordTo(
                    ReadWriteStream.Servirtium(root, name + "." + ec.requiredTestMethod.name)
                ).then(httpHandler)
                else -> throw IllegalArgumentException("Class is not an instance of: ${ServirtiumContract::name}")
            }
        }
}

/**
 * JUnit 5 extension for replaying HTTP traffic from disk in Servirtium format.
 */
class ServirtiumReplay(private val root: File = File(".")) : ParameterResolver {
    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supportedParam()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext): HttpHandler =
        with(ec.testInstance.get()) {
            when (this) {
                is ServirtiumContract -> {
                    val readWriteStream = ReadWriteStream.Servirtium(root, name + "." + ec.requiredTestMethod.name)
                    val zipped = readWriteStream.requests().zip(readWriteStream.responses()).iterator()
                    val count = AtomicInteger()

                    return { received: Request ->
                        val (expectedReq, response) = zipped.next()

                        assertEquals(
                            received.toString(),
                            received.removeHeadersNotIn(expectedReq).toString(),
                            "Unexpected request received for Interaction " + count.get()
                        )
                        response
                    }
                }
                else -> throw IllegalArgumentException("Class is not an instance of: ${ServirtiumContract::name}")
            }
        }

    private fun Request.removeHeadersNotIn(that: Request) =
        that.headers.fold(this) { reqToCheck, nextExpectedHeader ->
            reqToCheck
                .takeIf { it.header(nextExpectedHeader.first) != null }
                ?: reqToCheck.removeHeader(nextExpectedHeader.first)
        }
}

private fun ParameterContext.supportedParam() = parameter.parameterizedType.typeName ==
    "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"
