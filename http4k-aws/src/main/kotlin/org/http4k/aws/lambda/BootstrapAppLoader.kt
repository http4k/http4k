package org.http4k.aws.lambda

import org.http4k.core.HttpHandler

data class BootstrapException(val m: String?) : Exception(m)

object BootstrapAppLoader : AppLoader {
    val HTTP4K_BOOTSTRAP_CLASS = "HTTP4K_BOOTSTRAP_CLASS"

    override fun invoke(environment: Map<String, String>): HttpHandler {
        try {
            val loadClass = this.javaClass.classLoader.loadClass(environment[HTTP4K_BOOTSTRAP_CLASS])
            return (loadClass.getDeclaredField("INSTANCE").get(null) as AppLoader)(environment)
        } catch (e: ClassNotFoundException) {
            throw BootstrapException("Could not find AppLoader class: ${e.message}")
        } catch (e: NoSuchFieldException) {
            throw BootstrapException("AppLoader class should be an object singleton that implements ${AppLoader::class.qualifiedName}")
        }
    }
}
