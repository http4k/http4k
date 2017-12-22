package org.http4k.core

fun Response.public() = addCacheability(Cacheability.public)

private fun Response.addCacheability(cacheability: Cacheability): Response =
        replaceHeader("Cache-Control", cacheability(header("Cache-Control")))

fun Response.private() = addCacheability(Cacheability.private)

fun Response.noCache() = addCacheability(Cacheability.`no-cache`)

fun Response.onlyIfCached() = addCacheability(Cacheability.`only-if-cached`)


fun Response.mustRevalidate() = addCachingHeader("must-revalidate")

fun Response.noStore() = addCachingHeader("no-store")

fun Response.maxAge(seconds: Int) = addCachingHeader("max-age=$seconds")

private fun Response.addCachingHeader(value: String) =
        if (header("Cache-Control").isNullOrEmpty()) header("Cache-Control", value) else extendCachingHeader(value)

private fun Response.extendCachingHeader(additionalValue: String) =
        replaceHeader("Cache-Control", "${header("Cache-Control")}, $additionalValue")

private enum class Cacheability {
    public,
    `no-cache`,
    private,
    `only-if-cached`;

    operator fun invoke(currentValue: String?): String =
            currentValue?.let {
                val split = currentValue.split(",")
                (listOf(name) + split
                        .map { it.trim() }
                        .filterNot { values().map { it.name }.contains(it) }).joinToString(", ")
            } ?: name
}
