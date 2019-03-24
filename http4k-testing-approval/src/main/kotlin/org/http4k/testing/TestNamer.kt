package org.http4k.testing

import java.lang.reflect.Method

/**
 * Provides the identification of test case.
 */
interface TestNamer {
    fun nameFor(testClass: Class<*>, testMethod: Method): String

    companion object {
        val ClassAndMethod = object : TestNamer {
            override fun nameFor(testClass: Class<*>, testMethod: Method): String =
                testClass.packageName.replace('.', '/') + '/' + testClass.simpleName + "." + testMethod.name
        }
    }
}