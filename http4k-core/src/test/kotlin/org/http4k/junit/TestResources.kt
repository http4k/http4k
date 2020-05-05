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

class TestResources(private val resourcePrefixer: (Class<*>) -> String = { "${it.simpleName}_" }) : ParameterResolver {

    override fun supportsParameter(pc: ParameterContext, ec: ExtensionContext) = pc.parameter.type == ResourceLoader::class.java

    override fun resolveParameter(pc: ParameterContext, ec: ExtensionContext) =
        object : ResourceLoader {
            override fun bytes(name: String): ByteArray = stream(name).readAllBytes()

            override fun text(name: String): String = stream(name).reader().readText().trim()

            override fun stream(name: String): InputStream {
                val prefix = resourcePrefixer(ec.testClass.get())
                val resource = "/${ec.testClass.get().packageName.replace('.', '/')}/$prefix${ec.testMethod.get().name}_$name"
                return ec.testClass.get().getResourceAsStream(resource)
                    ?: throw IllegalStateException("Cannot find resource `$resource`")
            }
        }
}
