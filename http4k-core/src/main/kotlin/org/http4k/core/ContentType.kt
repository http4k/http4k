package org.http4k.core

import java.nio.charset.Charset

data class ContentType(val value: String, val directives: Parameters = emptyList()) {

    fun withNoDirectives() = copy(directives = emptyList())

    fun toHeaderValue() = (
        listOf(value) +
            directives
                .map { it.first + (it.second?.let { "=$it" } ?: "") }
        ).joinToString("; ")

    fun equalsIgnoringDirectives(that: ContentType): Boolean = withNoDirectives() == that.withNoDirectives()

    companion object {
        fun Text(value: String, charset: Charset? = Charsets.UTF_8) = ContentType(value, listOfNotNull(charset?.let { "charset" to charset.name().toLowerCase() }))
        fun MultipartFormWithBoundary(boundary: String): ContentType = ContentType("multipart/form-data", listOf("boundary" to boundary))
        fun MultipartMixedWithBoundary(boundary: String): ContentType = ContentType("multipart/mixed", listOf("boundary" to boundary))

        val APPLICATION_JSON = Text("application/json")
        val APPLICATION_FORM_URLENCODED = Text("application/x-www-form-urlencoded")
        val MULTIPART_FORM_DATA = Text("multipart/form-data")
        val MULTIPART_MIXED = Text("multipart/mixed")
        val APPLICATION_XML = Text("application/xml")
        val APPLICATION_PDF = Text("application/pdf")
        val TEXT_HTML = Text("text/html")
        val TEXT_YAML = Text("text/yaml")
        val TEXT_XML = Text("text/xml")
        val TEXT_PLAIN = Text("text/plain")
        val TEXT_EVENT_STREAM = Text("text/event-stream")
        val OCTET_STREAM = ContentType("application/octet-stream")
    }
}
