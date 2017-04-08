package org.reekwest.http.core

import javax.activation.MimetypesFileTypeMap

data class ContentType(val value: String) {

    companion object {
        private val extMap = MimetypesFileTypeMap(ContentType::class.java.getResourceAsStream("/META-INF/mime.types"))

        fun lookupFor(name: String): ContentType = ContentType(extMap.getContentType(name))

        val APPLICATION_JSON = ContentType("application/json")
        val APPLICATION_FORM_URLENCODED = ContentType("application/x-www-form-urlencoded")
        val APPLICATION_XML = ContentType("application/xml")
        val TEXT_HTML = ContentType("text/html")
        val TEXT_PLAIN = ContentType("text/plain")
        val TEXT_XML = ContentType("text/xml")
        val WILDCARD = ContentType("*/*")
    }
}
