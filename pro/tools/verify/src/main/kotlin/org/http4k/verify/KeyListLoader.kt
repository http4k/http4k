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

class KeyListLoader(
    private val url: Uri,
    private val log: (String) -> Unit,
    private val client: HttpHandler,
    private val cacheDir: File? = null
) {
    fun load(): CosignKeyList {
        val cached = cacheDir?.let { File(it, "cosign-keys.json") }

        if (cached != null && cached.exists()) {
            return Moshi.asA<CosignKeyList>(cached.readText()).validated()
        }

        log("Downloading key list from $url")
        val response = client(Request(GET, url))
        if (response.status != OK) error("Failed to download key list: ${response.status}")

        val keyList = response.json<CosignKeyList>().validated()

        cached?.also {
            it.parentFile?.mkdirs()
            it.writeText(response.bodyString())
        }

        return keyList
    }
}
