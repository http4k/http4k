package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.TrafficFilters
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Servirtium
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

class ServirtiumRecording(private val name: String, private val original: HttpHandler) : BeforeTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(context: ExtensionContext) =
        with(context) {
            store().put("http", TrafficFilters.RecordTo(ReadWriteStream.Servirtium(File("."),
                name + "." + requiredTestMethod.name)).then(original))
        }

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        parameterContext.parameter.parameterizedType.typeName ==
            "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext) =
        if (supportsParameter(parameterContext, context)) context.store()["http"] else null

    private fun ExtensionContext.store() = getStore(ExtensionContext.Namespace.create(name, requiredTestMethod.name))
}