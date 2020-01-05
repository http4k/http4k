package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.RegisterExtension
import java.lang.reflect.InvocationTargetException

class ReplayStoredContract {

    private val name = "org.http4k.testing.ClientContract"

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording(name) { Response(OK) }

    @TestFactory
    @Disabled
    fun `replay stored tests`(): List<DynamicTest> {
        val testClass = Class.forName(name)
        val instance = testClass.getDeclaredConstructor().newInstance()
        val handler = { _: Request -> Response(OK) }

        return testClass.methods
            .filter { it.annotations.any { it.annotationClass == Test::class } }
            .map {
                dynamicTest(it.name) {
                    try {
                        it.invoke(instance, handler)
                    } catch (e: InvocationTargetException) {
                        throw e.cause!!
                    }
                }
            }
    }
}