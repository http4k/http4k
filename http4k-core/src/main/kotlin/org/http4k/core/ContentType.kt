package org.http4k.core

import java.nio.charset.Charset

data class ContentType(val value: String, val directive: Pair<String, String>? = null) {

    companion object {

        fun Text(value: String, charset: Charset? = Charsets.UTF_8) = ContentType(value, charset?.let { "charset" to charset.name() })
        fun MultipartForm(boundary: String): ContentType = ContentType("multipart/form-data", "boundary" to boundary)

        val APPLICATION_JSON = Text("application/json")
        val APPLICATION_FORM_URLENCODED = Text("application/x-www-form-urlencoded")
        val APPLICATION_XML = Text("application/xml")
        val TEXT_HTML = Text("text/html")
        val TEXT_XML = Text("text/xml")
        val TEXT_PLAIN = Text("text/plain")
        val OCTET_STREAM = ContentType("application/octet-stream", null)

    }
}
