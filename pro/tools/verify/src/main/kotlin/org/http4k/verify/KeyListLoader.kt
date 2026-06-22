/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.Moshi
import org.http4k.format.Moshi.json
import java.io.File
import java.time.Clock
import java.time.Duration

class KeyListLoader(
    private val url: Uri,
    private val log: (String) -> Unit,
    private val client: HttpHandler,
    private val cacheDir: File? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofDays(1)
) {
    fun load(): CosignKeyList {
        val cached = cacheDir?.let { File(it, "cosign-keys.json") }

        if (cached != null && cached.exists() && isFresh(cached)) {
            return Moshi.asA<CosignKeyList>(cached.readText()).validated()
        }

        return try {
            download(cached)
        } catch (e: Exception) {
            if (cached != null && cached.exists()) {
                log("Failed to refresh key list (${e.message}); using stale cache from ${cached.absolutePath}")
                Moshi.asA<CosignKeyList>(cached.readText()).validated()
            } else {
                throw e
            }
        }
    }

    private fun isFresh(file: File): Boolean =
        clock.millis() - file.lastModified() < ttl.toMillis()

    private fun download(cached: File?): CosignKeyList {
        log("Downloading key list from $url")
        val response = client(Request(GET, url))
        if (response.status != OK) error("Failed to download key list: ${response.status}")

        val keyList = response.json<CosignKeyList>().validated()

        cached?.also {
            it.parentFile?.mkdirs()
            it.writeText(response.bodyString())
            it.setLastModified(clock.millis())
        }

        return keyList
    }
}
