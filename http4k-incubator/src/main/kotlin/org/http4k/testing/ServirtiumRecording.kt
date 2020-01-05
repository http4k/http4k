package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Servirtium
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

class ServirtiumRecording(private val name: String,
                          private val original: HttpHandler,
                          private val root: File = File(".")) : BeforeTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(ec: ExtensionContext) = ec.lookup(name).put("http", TrafficFilters.RecordTo(ReadWriteStream.Servirtium(root, name + "." + ec.requiredTestMethod.name)).then(original))

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supportedParam()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) =
        if (pc.supportedParam()) ec.lookup(name)["http"] else null
}

private fun ParameterContext.supportedParam() = parameter.parameterizedType.typeName ==
    "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

private fun ExtensionContext.lookup(name: String) = getStore(create(name, requiredTestMethod.name))
