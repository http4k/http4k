package org.http4k.routing

import java.io.File
import java.net.URL

interface ResourceLoader {
    fun load(path: String): URL?

    companion object {
        fun Classpath(basePackagePath: String = "/") = object : ResourceLoader {
            private val withStarting = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"
            private val finalBasePath = if (withStarting.endsWith("/")) withStarting else "$withStarting/"

            override fun load(path: String): URL? = javaClass.getResource(finalBasePath + path)
        }

        fun Directory(baseDir: String = ".") = object : ResourceLoader {
            private val finalBaseDir = if (baseDir.endsWith("/")) baseDir else "$baseDir/"

            override fun load(path: String): URL? =
                File(finalBaseDir, path).let { f -> if (f.exists() && f.isFile) f.toURI().toURL() else null }
        }

    }
}