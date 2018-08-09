package org.http4k.routing

import java.io.File



interface NewResourceLoader {

    fun resourceFor(path: String): Resource?

    companion object {
        fun Classpath(basePackagePath: String = "/") = object : NewResourceLoader {
            override fun resourceFor(path: String) = javaClass.getResource(finalBasePath + path)?.toResource()

            private val withStarting = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"
            private val finalBasePath = if (withStarting.endsWith("/")) withStarting else "$withStarting/"
        }

        fun Directory(baseDir: String) = object : NewResourceLoader {
            override fun resourceFor(path: String) = File(finalBaseDir, path).let { f ->
                if (f.exists() && f.isFile) f.toURI().toURL() else null
            }?.toResource()

            private val finalBaseDir = if (baseDir.endsWith("/")) baseDir else "$baseDir/"
        }
    }
}

