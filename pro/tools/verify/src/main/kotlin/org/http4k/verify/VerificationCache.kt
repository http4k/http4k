/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import java.io.File
import java.security.MessageDigest

object VerificationCache {
    private fun cacheDir(gradleUserHome: File) =
        File(gradleUserHome, "caches/http4k-verify").apply { mkdirs() }

    private fun cacheKey(group: String, module: String, version: String, artifactChecksum: String) =
        "$group:$module:$version:$artifactChecksum"

    private fun cacheFile(gradleUserHome: File) = File(cacheDir(gradleUserHome), "verified.txt")

    fun isVerified(gradleUserHome: File, group: String, module: String, version: String, artifact: File): Boolean {
        val checksum = sha256(artifact)
        val key = cacheKey(group, module, version, checksum)
        val cache = cacheFile(gradleUserHome)
        return cache.exists() && cache.readLines().contains(key)
    }

    fun markVerified(gradleUserHome: File, group: String, module: String, version: String, artifact: File) {
        val checksum = sha256(artifact)
        val key = cacheKey(group, module, version, checksum)
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
