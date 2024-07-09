package org.http4k.core

import org.http4k.filter.MaxAgeTtl
import org.http4k.filter.StaleIfErrorTtl
import org.http4k.filter.StaleWhenRevalidateTtl
import kotlin.time.Duration as KotlinDuration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

fun Response.public() = addCacheability(Cacheability.public)

fun Response.private() = addCacheability(Cacheability.private)

fun Response.noCache() = addCacheability("no-cache")

fun Response.onlyIfCached() = addCacheability("only-if-cached")

fun Response.mustRevalidate() = addCacheability("must-revalidate")

fun Response.noStore() = addCacheability("no-store")

fun Response.immutable() = addCacheability("immutable")

fun Response.maxAge() = getCacheControlDirectiveValue("max-age")

fun Response.maxAge(duration: JavaDuration) = replaceHeader("Cache-Control", MaxAgeTtl(duration).replaceIn(header("Cache-Control")))

fun Response.maxAge(duration: KotlinDuration) = maxAge(duration.toJavaDuration())

fun Response.staleWhileRevalidate() = getCacheControlDirectiveValue("stale-while-revalidate")

fun Response.staleWhileRevalidate(duration: JavaDuration) = replaceHeader("Cache-Control", StaleWhenRevalidateTtl(duration).replaceIn(header("Cache-Control")))

fun Response.staleWhileRevalidate(duration: KotlinDuration) = staleWhileRevalidate(duration.toJavaDuration())

fun Response.staleIfError() = getCacheControlDirectiveValue("stale-if-error")

fun Response.staleIfError(duration: JavaDuration) = replaceHeader("Cache-Control", StaleIfErrorTtl(duration).replaceIn(header("Cache-Control")))

fun Response.staleIfError(duration: KotlinDuration) = staleIfError(duration.toJavaDuration())

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

private fun Response.getCacheControlDirectiveValue(directive: String) =
    header("Cache-Control")?.let { Regex("$directive=(\\d+)").find(it) }?.groupValues?.lastOrNull()?.toLongOrNull()
