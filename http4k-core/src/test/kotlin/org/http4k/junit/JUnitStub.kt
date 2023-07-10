package org.http4k.junit

import org.http4k.core.HttpHandler
import org.http4k.util.proxy
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import java.lang.reflect.Parameter
import java.util.Optional

class JUnitStub(private val t: Any) : ExtensionContext by proxy(), ParameterContext by proxy() {
    private object Lookup {
        @Suppress("UNUSED_PARAMETER")
        fun handler(handler: HttpHandler): Nothing = TODO()
    }

    override fun getTestInstance() = Optional.of(t)
    override fun getTestMethod() = Optional.of(JUnitStub::class.java.getMethod("hashCode"))
    override fun getParameter(): Parameter = Lookup::class.java.methods.first().parameters[0]
}
