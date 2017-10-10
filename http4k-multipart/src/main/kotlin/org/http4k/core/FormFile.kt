package org.http4k.core

import java.io.InputStream

data class FormFile(val filename: String, val contentType: ContentType, val content: InputStream) {

    private data class Realised(val filename: String, val contentType: ContentType, val content: String)

    private val realised by lazy { Realised(filename, contentType, content.use { String(it.readBytes()) }) }

    override fun toString(): String = realised.toString()

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is FormFile? -> false
        else -> realised == other?.realised
    }

    override fun hashCode(): Int = realised.hashCode()
}