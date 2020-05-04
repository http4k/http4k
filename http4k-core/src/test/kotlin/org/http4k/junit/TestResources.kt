package org.http4k.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.InputStream

interface ResourceLoader {
    fun text(name: String): String
    fun bytes(name: String): ByteArray
    fun stream(name: String): InputStream
}

class TestResources : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type == ResourceLoader::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        object : ResourceLoader {
            override fun text(name: String): String = stream(name).reader().readText().trim()

            override fun bytes(name: String): ByteArray = stream(name).readAllBytes()

            override fun stream(name: String): InputStream {
                val resource = "/${extensionContext.testClass.get().packageName.replace('.', '/')}/${extensionContext.testClass.get().simpleName}_${extensionContext.testMethod.get().name}_$name"
                return extensionContext.testClass.get().getResourceAsStream(resource)
                    ?: throw IllegalStateException("Cannot find resource `$resource`")
            }
        }
}
