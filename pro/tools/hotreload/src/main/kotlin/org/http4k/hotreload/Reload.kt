package org.http4k.hotreload

import java.net.URL
import java.nio.file.Path

/**
 * Responsible for reloading an application when the classpath changes.
 */
fun interface Reload<T> {
    operator fun invoke(className: String, paths: List<Path>): T

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> Classpath(): Reload<T> = Reload { className, paths ->
            val classLoader = HotReloadClassLoader(paths.map { it.toUri().toURL() }
                .toTypedArray<URL>())
                .also { Thread.currentThread().contextClassLoader = it }

            val appClass = Class.forName(className, true, classLoader)

            Class.forName(className, true, classLoader)
                .getDeclaredMethod("create")
                .invoke(appClass.getDeclaredConstructor().newInstance()) as T
        }
    }
}
