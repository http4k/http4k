/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import java.io.File
import java.security.MessageDigest

object VerificationCache {
    private fun cacheFile(gradleUserHome: File) =
        File(gradleUserHome, "caches/http4k-verify").apply { mkdirs() }
            .let { File(it, "verified.txt") }

    private fun cacheKey(label: String, artifact: File) = "$label:${sha256(artifact)}"

    fun isVerified(gradleUserHome: File, label: String, artifact: File): Boolean {
        val key = cacheKey(label, artifact)
        val cache = cacheFile(gradleUserHome)
        return cache.exists() && cache.readLines().contains(key)
    }

    fun markVerified(gradleUserHome: File, label: String, artifact: File) {
        val key = cacheKey(label, artifact)
        val cache = cacheFile(gradleUserHome)
        if (!cache.exists() || !cache.readLines().contains(key)) {
            cache.appendText("$key\n")
        }
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
}
