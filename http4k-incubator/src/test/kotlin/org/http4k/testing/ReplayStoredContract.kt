package org.http4k.testing

import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.traffic.ReadWriteStream
import org.http4k.traffic.Servirtium
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class ReplayStoredContract {

    private val name = "org.http4k.testing.ClientContract"
    private val testClass = Class.forName(name)
    private val instance = Proxy.newProxyInstance(testClass.classLoader, arrayOf(testClass)) { _, _, _ -> null }

    @TestFactory
    fun `replay stored tests`(): List<DynamicTest> {
        val mainName = "getName"().invokeWithArguments() as String
        return testClass.methods
            .filter { it.annotations.any { it.annotationClass == Test::class } }
            .map {
                dynamicTest(it.name) {
                    val readWriteStream = ReadWriteStream.Servirtium(File("."), mainName + "." + it.name)
                    val zipped = readWriteStream.requests().zip(readWriteStream.responses()).iterator()
                    val count = AtomicInteger()

                    val handler = Filter {
                        {
                            val (request, response) = zipped.next()
                            assertEquals(it.toString(), request.toString(), "Unexpected request received for Interaction " + count.get())
                            response
                        }
                    }.then { throw IllegalAccessException("Should never get here") }


                    it.name().invokeWithArguments(handler)
                }
            }
    }

    private operator fun String.invoke(): MethodHandle {
        val method = testClass.methods.toList().first { m -> m.name == this }
        return MethodHandles.privateLookupIn(testClass, MethodHandles.lookup())
            .unreflectSpecial(method, method.declaringClass)
            .bindTo(instance)
    }
}