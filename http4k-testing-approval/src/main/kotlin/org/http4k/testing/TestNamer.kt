package org.http4k.testing

import java.lang.reflect.Method

interface TestNamer {
    fun nameFor(testClass: Class<*>, testMethod: Method): String

    companion object {
        val Simple = object : TestNamer {
            override fun nameFor(testClass: Class<*>, testMethod: Method): String =
                testClass.simpleName + "." + testMethod.name
        }
    }
}