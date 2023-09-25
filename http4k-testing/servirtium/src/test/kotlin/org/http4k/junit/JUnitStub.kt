package org.http4k.junit

import dev.forkhandles.mock4k.mock
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import java.lang.reflect.Parameter
import java.util.Optional

class JUnitStub(private val t: Any) : ExtensionContext by mock(),
    ParameterContext by mock() {
    private object Lookup {
        @Suppress("UNUSED_PARAMETER")
        fun handler(handler: HttpHandler): Nothing = TODO()
    }

    override fun getTestInstance() = Optional.of(t)
    override fun getTestMethod() = Optional.of(ServirtiumReplayTest::class.java.getMethod("hashCode"))
    override fun getParameter(): Parameter = Lookup::class.java.methods.first().parameters[0]
}
