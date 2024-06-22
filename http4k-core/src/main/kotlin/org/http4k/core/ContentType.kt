package org.http4k.core

import java.nio.charset.Charset
import java.util.Locale.getDefault
import kotlin.text.Charsets.UTF_8

data class ContentType(val value: String, val directives: Parameters = emptyList()) {

    fun withNoDirectives() = copy(directives = emptyList())

    fun toHeaderValue() = (
        listOf(value) +
            directives
                .map { it.first + (it.second?.let { "=$it" } ?: "") }
        ).joinToString("; ")

    fun equalsIgnoringDirectives(that: ContentType): Boolean = withNoDirectives() == that.withNoDirectives()

    companion object {
        fun Text(value: String, charset: Charset? = UTF_8) = ContentType(value, listOfNotNull(charset?.let {
            "charset" to charset.name().lowercase(getDefault())
        }))

        fun MultipartFormWithBoundary(boundary: String): ContentType = ContentType("multipart/form-data", listOf("boundary" to boundary))
        fun MultipartMixedWithBoundary(boundary: String): ContentType = ContentType("multipart/mixed", listOf("boundary" to boundary))

        val APPLICATION_FORM_URLENCODED = Text("application/x-www-form-urlencoded")
        val APPLICATION_JSON = Text("application/json")
        val APPLICATION_ND_JSON = Text("application/x-ndjson")
        val APPLICATION_PDF = Text("application/pdf")
        val APPLICATION_XML = Text("application/xml")
        val APPLICATION_YAML = Text("application/yaml")
        val MULTIPART_FORM_DATA = Text("multipart/form-data")
        val MULTIPART_MIXED = Text("multipart/mixed")
        val OCTET_STREAM = ContentType("application/octet-stream")
        val TEXT_CSV = Text("text/csv")
        val TEXT_EVENT_STREAM = Text("text/event-stream")
        val TEXT_PLAIN = Text("text/plain")
        val TEXT_HTML = Text("text/html")
        val TEXT_XML = Text("text/xml")
        val TEXT_YAML = Text("text/yaml")
    }
}
