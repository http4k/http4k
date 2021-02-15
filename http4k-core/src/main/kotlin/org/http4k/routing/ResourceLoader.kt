package org.http4k.routing

import java.io.File
import java.net.URL

/**
 * Looks up contents of a resource path.
 *
 * WARNING: A ResourceLoader serves ANY resource it finds within it's structure. This means that you should be
 * VERY careful to limit what resources it has access to.
 */
fun interface ResourceLoader {
    fun load(path: String): URL?

    companion object {
        fun Classpath(basePackagePath: String = "/", muteWarning: Boolean = false) = object : ResourceLoader {

            private val withStarting = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"

            private val finalBasePath = if (withStarting.endsWith("/")) withStarting else "$withStarting/"

            init {
                if (!muteWarning && finalBasePath == "/") {
                    System.err.println(
                        """|****************************************************************************
                            |WARNING - http4k Classpath ResourceLoader is configured to serve ALL files
                            |from the root of the Java classpath.
                            |For security serve content from a non-code package eg. /public, or mute this 
                            |warning using the flag on construction.
                            |****************************************************************************"""
                            .trimMargin()
                    )
                }
            }

            override fun load(path: String): URL? = javaClass.getResource(finalBasePath + path)
        }

        fun Directory(baseDir: String = ".") = object : ResourceLoader {
            private val finalBaseDir = if (baseDir.endsWith("/")) baseDir else "$baseDir/"

            override fun load(path: String) = File(finalBaseDir, path)
                .let {
                    if (it.exists() && it.isFile && !it.toPath().normalize().startsWith("..")) it.toURI().toURL()
                    else null
                }
        }
    }
}
