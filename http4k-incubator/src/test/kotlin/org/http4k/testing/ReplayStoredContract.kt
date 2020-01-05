package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.RegisterExtension
import java.lang.invoke.MethodHandles
import java.lang.reflect.Proxy

class ReplayStoredContract {

    private val name = "org.http4k.testing.ClientContract"

    @JvmField
    @RegisterExtension
    val replay = ServirtiumReplay(name)

    @TestFactory
    fun `replay stored tests`(): List<DynamicTest> {
        val testClass = Class.forName(name)

        val handler = { _: Request -> Response(OK) }

        val instance = Proxy.newProxyInstance(testClass.classLoader, arrayOf(testClass)) { _, _, _ -> null }
        return testClass.methods
            .filter { it.annotations.any { it.annotationClass == Test::class } }
            .map {
                dynamicTest(it.name) { it.name(testClass, instance, handler) }
            }
    }

    private operator fun String.invoke(testClass: Class<*>, instance: Any, handler: (Request) -> Response) {
        val method = testClass.methods.toList().first { m -> m.name == this }
        MethodHandles.privateLookupIn(testClass, MethodHandles.lookup())
            .unreflectSpecial(method, method.declaringClass)
            .bindTo(instance)
            .invokeWithArguments(handler)
    }
}