/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.storyboard.util.gzipBase64Decode
import org.http4k.storyboard.util.gzipBase64DecodeBytes
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LocalAssetCollectorTest {

    private val pngBytes = ByteArray(8) { it.toByte() }
    private val largeBytes = ByteArray(2_000) { (it % 256).toByte() }

    private fun handler(routes: Map<String, Response>): HttpHandler = { req ->
        routes[req.uri.path] ?: Response(NOT_FOUND)
    }

    private fun bytes(b: ByteArray) = Response(OK).body(MemoryBody(b))
    private fun css(body: String) = Response(OK).with(Header.CONTENT_TYPE of ContentType("text/css")).body(body)

    @Test
    fun `collects local assets referenced from the html`() {
        val routes = mapOf(
            "/img.png" to bytes(pngBytes),
            "/main.css" to css("body { color: red; }")
        )
        val html = """
            <html><head>
              <link rel="stylesheet" href="/main.css">
            </head><body>
              <img src="/img.png">
            </body></html>
        """.trimIndent()

        val assets = LocalAssetCollector(handler(routes)).collect(html, "https://example.com/page")

        assertThat(assets.keys, equalTo(setOf("/main.css", "/img.png")))
        assertThat(assets["/img.png"]!!.gzipBase64DecodeBytes().toList(), equalTo(pngBytes.toList()))
        assertThat(assets["/main.css"]!!.gzipBase64Decode(), equalTo("body { color: red; }"))
    }

    @Test
    fun `skips external http and https urls and other schemes`() {
        val calls = AtomicInteger()
        val http: HttpHandler = { calls.incrementAndGet(); Response(OK).body("ignored") }
        val html = """
            <html><body>
              <img src="https://cdn.example.com/external.png">
              <img src="http://other.example.com/external2.png">
              <script src="//cdn.example.com/lib.js"></script>
              <a href="mailto:hi@example.com">x</a>
              <img src="data:image/png;base64,AAAA">
              <a href="#anchor">x</a>
              <a href="javascript:void(0)">x</a>
            </body></html>
        """.trimIndent()

        val assets = LocalAssetCollector(http).collect(html, "https://example.com/page")

        assertThat(assets, equalTo(emptyMap()))
        assertThat(calls.get(), equalTo(0))
    }

    @Test
    fun `recurses one level into CSS url refs`() {
        val routes = mapOf(
            "/style.css" to css("body { background: url('/bg.png'); }"),
            "/bg.png" to bytes(pngBytes)
        )
        val html = """<html><head><link href="/style.css" rel="stylesheet"></head><body></body></html>"""

        val assets = LocalAssetCollector(handler(routes)).collect(html, "https://example.com/page")

        assertThat(assets.keys, equalTo(setOf("/style.css", "/bg.png")))
    }

    @Test
    fun `skips assets that return non-2xx`() {
        val routes = mapOf("/exists.png" to bytes(pngBytes))
        val html = """
            <html><body>
              <img src="/exists.png">
              <img src="/missing.png">
            </body></html>
        """.trimIndent()

        val assets = LocalAssetCollector(handler(routes)).collect(html, "https://example.com/page")

        assertThat(assets.keys, equalTo(setOf("/exists.png")))
    }

    @Test
    fun `fetches each url at most once`() {
        val calls = AtomicInteger()
        val http: HttpHandler = { calls.incrementAndGet(); bytes(pngBytes) }
        val html = """
            <html><body>
              <img src="/img.png">
              <img src="/img.png">
              <img src="/img.png">
            </body></html>
        """.trimIndent()

        val assets = LocalAssetCollector(http).collect(html, "https://example.com/page")

        assertThat(assets.keys, equalTo(setOf("/img.png")))
        assertThat(calls.get(), equalTo(1))
    }

    @Test
    fun `skips assets larger than maxBytes`() {
        val routes = mapOf("/big.bin" to bytes(largeBytes))
        val html = """<html><body><img src="/big.bin"></body></html>"""

        val assets = LocalAssetCollector(handler(routes), maxBytes = 100).collect(html, "https://example.com/page")

        assertThat(assets, equalTo(emptyMap()))
    }

    @Test
    fun `resolves relative paths against base url`() {
        val routes = mapOf("/section/img.png" to bytes(pngBytes))
        val html = """<html><body><img src="img.png"></body></html>"""

        val assets = LocalAssetCollector(handler(routes)).collect(html, "https://example.com/section/page")

        assertThat(assets.keys, hasSize(equalTo(1)))
        assertThat(assets.keys.single(), equalTo("img.png"))
    }

    @Test
    fun `empty html produces no assets`() {
        val calls = AtomicInteger()
        val http: HttpHandler = { calls.incrementAndGet(); Response(OK) }

        val assets = LocalAssetCollector(http).collect("", "https://example.com/")

        assertThat(assets, equalTo(emptyMap()))
        assertThat(calls.get(), equalTo(0))
    }
}
