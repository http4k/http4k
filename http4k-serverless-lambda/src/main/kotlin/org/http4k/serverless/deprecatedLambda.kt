package org.http4k.serverless

import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts

@Deprecated("Extend LambdaFunction instead of this reflection-based approach.")
object BootstrapAppLoader : AppLoaderWithContexts {
    const val HTTP4K_BOOTSTRAP_CLASS = "HTTP4K_BOOTSTRAP_CLASS"

    override fun invoke(environment: Map<String, String>, contexts: RequestContexts): HttpHandler = try {
        val loadClass = javaClass.classLoader.loadClass(environment[HTTP4K_BOOTSTRAP_CLASS])

        when (val appLoaderInstance = loadClass.getDeclaredField("INSTANCE").get(null)) {
            is AppLoader -> appLoaderInstance(environment)
            is AppLoaderWithContexts -> appLoaderInstance(environment, contexts)
            else -> throw InvalidAppLoaderException()
        }
    } catch (e: ClassNotFoundException) {
        throw CouldNotFindAppLoaderException(e)
    } catch (e: NoSuchFieldException) {
        throw InvalidAppLoaderException(e)
    }
}

open class BootstrapException(m: String?, cause: Throwable? = null) : RuntimeException(m, cause)

class CouldNotFindAppLoaderException(cause: ClassNotFoundException) : BootstrapException("Could not find AppLoader class: ${cause.message}", cause)

class InvalidAppLoaderException(cause: Exception? = null) : BootstrapException(
    "AppLoader class should be an object singleton that implements either ${AppLoaderWithContexts::class.qualifiedName} (recommended) " +
        "or ${AppLoader::class.qualifiedName}",
    cause
)
