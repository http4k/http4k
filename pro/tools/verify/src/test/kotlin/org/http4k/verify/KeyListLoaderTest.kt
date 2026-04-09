/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.format.Moshi.json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class KeyListLoaderTest {

    @TempDir
    lateinit var cacheDir: File

    private val keyList = CosignKeyList(
        schemaVersion = 1,
        keys = listOf(
            CosignKey("key-2025", "sha256:abc", "pem", KeyStatus.active, Instant.parse("2025-01-01T00:00:00Z"))
        )
    )

    @Test
    fun `downloads key list from URL`() {
        val logged = mutableListOf<String>()
        val loader = KeyListLoader(
            url = Uri.of("https://http4k.org/.well-known/cosign-keys.json"),
            log = { logged += it },
            client = { Response(OK).json(keyList) },
            cacheDir = cacheDir
        )

        val result = loader.load()

        assertThat(result.schemaVersion, equalTo(1))
        assertThat(result.keys.size, equalTo(1))
        assertThat(result.keys[0].kid, equalTo("key-2025"))
        assertThat(logged.size, equalTo(1))
    }

    @Test
    fun `caches key list to disk`() {
        var downloadCount = 0
        val client = { _: org.http4k.core.Request ->
            downloadCount++
            Response(OK).json(keyList)
        }

        val loader = KeyListLoader(
            url = Uri.of("https://http4k.org/.well-known/cosign-keys.json"),
            log = {},
            client = client,
            cacheDir = cacheDir
        )

        loader.load()
        loader.load()

        assertThat(downloadCount, equalTo(1))
    }

    @Test
    fun `fails on non-OK response`() {
        val loader = KeyListLoader(
            url = Uri.of("https://http4k.org/.well-known/cosign-keys.json"),
            log = {},
            client = { Response(NOT_FOUND) },
            cacheDir = cacheDir
        )

        assertThat({ loader.load() }, throws<IllegalStateException>())
    }

    @Test
    fun `rejects unsupported schema version`() {
        val badJson = Moshi.asFormatString(CosignKeyList(schemaVersion = 99, keys = emptyList()))
        val loader = KeyListLoader(
            url = Uri.of("https://http4k.org/.well-known/cosign-keys.json"),
            log = {},
            client = { Response(OK).body(badJson) },
            cacheDir = cacheDir
        )

        assertThat({ loader.load() }, throws<IllegalStateException>())
    }
}
