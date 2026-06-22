/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.format.Moshi.json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class KeyListLoaderTest {

    @TempDir
    lateinit var cacheDir: File

    private val keyList = CosignKeyList(
        schemaVersion = 1,
        keys = listOf(
            CosignKey("key-2025", "sha256:abc", "pem", KeyStatus.active, Instant.parse("2025-01-01T00:00:00Z"))
        )
    )

    private val start = Instant.parse("2026-06-20T00:00:00Z")
    private var now = start
    private val clock = object : Clock() {
        override fun instant() = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
    }

    @Test
    fun `downloads key list from URL`() {
        val logged = mutableListOf<String>()
        val loader = loader(client = { Response(OK).json(keyList) }, log = { logged += it })

        val result = loader.load()

        assertThat(result.schemaVersion, equalTo(1))
        assertThat(result.keys.size, equalTo(1))
        assertThat(result.keys[0].kid, equalTo("key-2025"))
        assertThat(logged.size, equalTo(1))
    }

    @Test
    fun `serves cached key list within TTL window`() {
        var downloadCount = 0
        val loader = loader(client = {
            downloadCount++
            Response(OK).json(keyList)
        })

        loader.load()
        now = start.plus(Duration.ofHours(23))
        loader.load()

        assertThat(downloadCount, equalTo(1))
    }

    @Test
    fun `re-downloads key list when cached copy is older than TTL`() {
        var downloadCount = 0
        val loader = loader(client = {
            downloadCount++
            Response(OK).json(keyList)
        })

        loader.load()
        now = start.plus(Duration.ofHours(25))
        loader.load()

        assertThat(downloadCount, equalTo(2))
    }

    @Test
    fun `falls back to stale cache when download fails`() {
        var downloadCount = 0
        val logged = mutableListOf<String>()
        val loader = loader(
            client = {
                downloadCount++
                if (downloadCount == 1) Response(OK).json(keyList) else throw RuntimeException("network down")
            },
            log = { logged += it }
        )

        loader.load()
        now = start.plus(Duration.ofHours(25))
        val result = loader.load()

        assertThat(result.keys[0].kid, equalTo("key-2025"))
        assertThat(logged.any { it.contains("stale") || it.contains("Failed") }, equalTo(true))
    }

    @Test
    fun `fails on non-OK response`() {
        val loader = loader(client = { Response(NOT_FOUND) })

        assertThat({ loader.load() }, throws<IllegalStateException>())
    }

    @Test
    fun `rejects unsupported schema version`() {
        val badJson = Moshi.asFormatString(CosignKeyList(schemaVersion = 99, keys = emptyList()))
        val loader = loader(client = { Response(OK).body(badJson) })

        assertThat({ loader.load() }, throws<IllegalStateException>())
    }

    @Test
    fun `propagates download error when no cache is present`() {
        val loader = loader(client = { throw RuntimeException("network down") })

        assertThat({ loader.load() }, throws<RuntimeException>())
    }

    private fun loader(
        client: (Request) -> Response,
        log: (String) -> Unit = {}
    ) = KeyListLoader(
        url = Uri.of("https://http4k.org/.well-known/cosign-keys.json"),
        log = log,
        client = client,
        cacheDir = cacheDir,
        clock = clock
    )
}
