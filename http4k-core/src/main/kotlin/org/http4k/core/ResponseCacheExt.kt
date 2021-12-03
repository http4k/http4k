package org.http4k.core

import org.http4k.filter.MaxAgeTtl
import org.http4k.filter.StaleIfErrorTtl
import org.http4k.filter.StaleWhenRevalidateTtl
import java.time.Duration

fun Response.public() = addCacheability(Cacheability.public)

fun Response.private() = addCacheability(Cacheability.private)

fun Response.noCache() = addCacheability("no-cache")

fun Response.onlyIfCached() = addCacheability("only-if-cached")

fun Response.mustRevalidate() = addCacheability("must-revalidate")

fun Response.noStore() = addCacheability("no-store")

fun Response.maxAge(duration: Duration) = replaceHeader("Cache-Control", MaxAgeTtl(duration).replaceIn(header("Cache-Control")))

fun Response.staleWhileRevalidate(duration: Duration) = replaceHeader("Cache-Control", StaleWhenRevalidateTtl(duration).replaceIn(header("Cache-Control")))

fun Response.staleIfError(duration: Duration) = replaceHeader("Cache-Control", StaleIfErrorTtl(duration).replaceIn(header("Cache-Control")))

private fun Response.addCacheability(value: String): Response =
    replaceHeader("Cache-Control", value.ensureOnlyOnceIn(header("Cache-Control")))

private fun Response.addCacheability(cacheability: Cacheability): Response =
    replaceHeader("Cache-Control", cacheability(header("Cache-Control")))

private enum class Cacheability {
    public,
    private;

    operator fun invoke(currentValue: String?): String =
        currentValue?.let {
            val split = currentValue.split(",")
            (listOf(name) + split
                .map(String::trim)
                .filterNot { values().map { it.name }.contains(it) }).joinToString(", ")
        } ?: name
}

private fun String.ensureOnlyOnceIn(currentValue: String?): String =
    currentValue?.split(",")?.map(String::trim)?.toSet()?.plus(this)?.joinToString(", ") ?: this
