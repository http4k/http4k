package org.http4k.testing

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
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

abstract class ServirtiumExtension(private val name: String,
                                   private val fn: (ExtensionContext) -> HttpHandler
) : BeforeTestExecutionCallback, ParameterResolver {

    override fun beforeTestExecution(ec: ExtensionContext) = ec.lookup(name).put("http", fn(ec))

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.supports()

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) =
        if (pc.supports()) ec.lookup(name)["http"] else null

    private fun ParameterContext.supports(): Boolean {
        println(parameter.parameterizedType.typeName)
        return parameter.parameterizedType.typeName ==
            "kotlin.jvm.functions.Function1<? super org.http4k.core.Request, ? extends org.http4k.core.Response>"
    }

    private fun ExtensionContext.lookup(name: String) = getStore(create(name, requiredTestMethod.name))
}

class ServirtiumRecording(private val name: String, original: HttpHandler) : ServirtiumExtension(name,
    {
        TrafficFilters.RecordTo(ReadWriteStream.Servirtium(File("."), name + "." + it.requiredTestMethod.name)).then(original)
    }
)

class ServirtiumReplay(private val name: String) : ServirtiumExtension(name,
    {
        TrafficFilters.RecordTo(ReadWriteStream.Servirtium(File("."), name + "." + it.requiredTestMethod.name)).then {
            Response(OK)
        }
    }
)
