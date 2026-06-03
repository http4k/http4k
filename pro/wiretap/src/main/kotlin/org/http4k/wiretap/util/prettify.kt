/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import org.jsoup.Jsoup
import java.io.StringReader
import java.io.StringWriter
import javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD
import javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET
import javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

fun formatBody(body: String, contentType: String): String {
    if (body.isBlank()) return body
    val ct = contentType.lowercase()
    return when {
        ct.contains("json") -> runCatching { Json.prettify(body) }.getOrDefault(body).normalizeLineEndings()
        ct.contains("html") -> runCatching { Jsoup.parse(body).html() }.getOrDefault(body).normalizeLineEndings()
        ct.contains("xml") -> runCatching { prettyPrintXml(body) }.getOrDefault(body).normalizeLineEndings()
        ct.contains("event-stream") -> body.normalizeLineEndings()
        ct.startsWith("text/") -> body
        ct.isBlank() -> body
        else -> "<<stream>>"
    }
}

private fun String.normalizeLineEndings() = replace("\r\n", "\n").replace("\r", "\n")

private fun prettyPrintXml(input: String): String {
    val factory = TransformerFactory.newInstance().apply {
        setFeature(FEATURE_SECURE_PROCESSING, true)
        setAttribute(ACCESS_EXTERNAL_DTD, "")
        setAttribute(ACCESS_EXTERNAL_STYLESHEET, "")
    }
    val transformer = factory.newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    val result = StreamResult(StringWriter())
    transformer.transform(StreamSource(StringReader(input)), result)
    return result.writer.toString().trim()
}
