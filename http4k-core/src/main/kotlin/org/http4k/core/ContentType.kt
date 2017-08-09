package org.http4k.core

data class ContentType(val value: String) {

    companion object {
        val APPLICATION_JSON = ContentType("application/json")
        val APPLICATION_FORM_URLENCODED = ContentType("application/x-www-form-urlencoded")
        val APPLICATION_XML = ContentType("application/xml")
        val TEXT_HTML = ContentType("text/html")
        val TEXT_XML = ContentType("text/xml")
        val TEXT_PLAIN = ContentType("text/plain")
        val OCTET_STREAM = ContentType("application/octet-stream")
    }
}
