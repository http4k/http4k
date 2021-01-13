package org.http4k.testing

import java.lang.reflect.Method

/**
 * Provides the identification of test case.
 */
fun interface TestNamer {
    fun nameFor(testClass: Class<*>, testMethod: Method): String

    companion object {
        val ClassAndMethod = TestNamer { testClass, testMethod ->
            testClass.`package`.name.replace('.', '/') + '/' + testClass.simpleName + "." + testMethod.name }

        val MethodOnly = TestNamer { testClass, testMethod ->
            testClass.`package`.name.replace('.', '/') + '/' + testMethod.name }
    }
}
