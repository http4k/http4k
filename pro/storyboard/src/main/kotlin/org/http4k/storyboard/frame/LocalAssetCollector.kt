/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.storyboard.DomAsset
import org.http4k.storyboard.util.gzipBase64Encode
import org.jsoup.Jsoup
import java.net.URI

class LocalAssetCollector(
    private val http: HttpHandler,
    private val maxBytes: Int = 2_000_000,
    private val mimeTypes: MimeTypes = MimeTypes()
) {
    fun collect(html: String, baseUrl: String?): Map<String, DomAsset> {
        if (html.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, DomAsset>()
        val visited = mutableSetOf<String>()
        scanHtml(html).forEach { fetchInto(it, baseUrl, out, visited, depth = 0) }
        return out
    }

    private fun fetchInto(
        originalUrl: String,
        baseUrl: String?,
        out: MutableMap<String, DomAsset>,
        visited: MutableSet<String>,
        depth: Int
    ) {
        if (!visited.add(originalUrl)) return
        if (!isLocal(originalUrl)) return
        val requestUrl = resolve(originalUrl, baseUrl) ?: return
        val response = runCatching { http(Request(GET, requestUrl)) }.getOrNull() ?: return
        if (!response.status.successful) return
        val bytes = runCatching { response.body.payload.array().copyOf(response.body.payload.remaining()) }
            .getOrNull() ?: return
        if (bytes.size > maxBytes) return
        val mimeType = mimeFor(originalUrl, response.header("Content-Type"))
        out[originalUrl] = DomAsset(mimeType, bytes.gzipBase64Encode())
        if (depth < 1 && mimeType.startsWith("text/css")) {
            scanCss(String(bytes, Charsets.UTF_8))
                .forEach { fetchInto(it, requestUrl, out, visited, depth + 1) }
        }
    }

    private fun mimeFor(url: String, contentTypeHeader: String?): String {
        val fromHeader = contentTypeHeader?.substringBefore(';')?.trim()
        if (!fromHeader.isNullOrEmpty()) return fromHeader
        val pathOnly = url.substringBefore('?').substringBefore('#')
        return mimeTypes.forFile(pathOnly).value
    }

    companion object {
        private val cssUrlRegex = Regex("""url\(\s*['"]?([^)'"]+)['"]?\s*\)""")
        private val nonLocalPrefixes = listOf(
            "http://", "https://", "data:", "mailto:", "javascript:", "#", "about:", "blob:", "tel:", "//"
        )

        private fun isLocal(url: String): Boolean {
            if (url.isBlank()) return false
            val trimmed = url.trim()
            if (trimmed.startsWith("\${")) return false
            return nonLocalPrefixes.none { trimmed.startsWith(it, ignoreCase = true) }
        }

        private fun resolve(url: String, baseUrl: String?): String? = runCatching {
            if (baseUrl.isNullOrBlank()) url else URI(baseUrl).resolve(url).toString()
        }.getOrNull()

        private fun scanHtml(html: String): List<String> {
            val doc = Jsoup.parse(html)
            val refs = mutableListOf<String>()
            doc.select("img[src], script[src], source[src], audio[src], video[src], iframe[src]")
                .forEach { refs.add(it.attr("src")) }
            doc.select("link[href]").forEach { refs.add(it.attr("href")) }
            doc.select("style").forEach { refs.addAll(scanCss(it.data())) }
            return refs.filter { it.isNotBlank() }
        }

        private fun scanCss(css: String): List<String> =
            cssUrlRegex.findAll(css).map { it.groupValues[1] }.toList()
    }
}
