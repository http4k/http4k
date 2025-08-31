package org.http4k.core

import java.nio.charset.Charset
import java.util.Locale
import java.util.Locale.ROOT
import java.util.Locale.getDefault
import kotlin.text.Charsets.UTF_8

data class ContentType(val value: String, val directives: Parameters = emptyList()) {

    fun withNoDirectives() = copy(directives = emptyList())

    fun withoutCharset() = copy(directives = directives.filter { it.first.lowercase(ROOT) != "charset" })

    fun toHeaderValue() = (
        listOf(value) +
            directives
                .map { it.first + (it.second?.let { "=$it" } ?: "") }
        ).joinToString("; ")

    fun equalsIgnoringDirectives(that: ContentType): Boolean = withNoDirectives() == that.withNoDirectives()

    fun structure() = Regex("\\+(\\w*)$").find(value)?.groups?.get(1)?.value

    @Suppress("unused")
    companion object {
        fun Text(value: String, charset: Charset? = UTF_8) = ContentType(value, listOfNotNull(charset?.let {
            "charset" to charset.name().lowercase(ROOT)
        }))

        fun MultipartFormWithBoundary(boundary: String): ContentType = ContentType("multipart/form-data", listOf("boundary" to boundary))
        fun MultipartMixedWithBoundary(boundary: String): ContentType = ContentType("multipart/mixed", listOf("boundary" to boundary))

        val APPLICATION_FORM_URLENCODED = Text("application/x-www-form-urlencoded")
        val APPLICATION_JSON = Text("application/json")
        val APPLICATION_ND_JSON = Text("application/x-ndjson")
        val APPLICATION_JRD_JSON = Text("application/jrd+json")
        val APPLICATION_LD_JSON = Text("application/ld+json")
        val APPLICATION_PDF = ContentType("application/pdf")
        val APPLICATION_XML = Text("application/xml")
        val APPLICATION_YAML = Text("application/yaml")
        val APPLICATION_ZIP = ContentType("application/zip")
        val MULTIPART_FORM_DATA = ContentType("multipart/form-data")
        val MULTIPART_MIXED = ContentType("multipart/mixed")
        val OCTET_STREAM = ContentType("application/octet-stream")
        val TEXT_CSV = Text("text/csv")
        val TEXT_EVENT_STREAM = Text("text/event-stream")
        val TEXT_PLAIN = Text("text/plain")
        val TEXT_HTML = Text("text/html")
        val TEXT_XML = Text("text/xml")
        val TEXT_YAML = Text("text/yaml")
        val IMAGE_PNG = ContentType("image/png")
        val IMAGE_JPG = ContentType("image/jpeg")
    }
}
