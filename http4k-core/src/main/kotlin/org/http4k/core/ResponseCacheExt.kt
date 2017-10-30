package org.http4k.core

fun Response.public() = addCachingHeader("public")

fun Response.private() = addCachingHeader("private")

fun Response.noCache() = addCachingHeader("no-cache")

fun Response.onlyIfCached() = addCachingHeader("only-if-cached")

fun Response.mustRevalidate() = addCachingHeader("must-revalidate")

fun Response.noStore() = addCachingHeader("no-store")

fun Response.noTransform() = addCachingHeader("no-transform")

fun Response.proxyRevalidate() = addCachingHeader("proxy-revalidate")

fun Response.maxAge(seconds: Int) = addCachingHeader("max-age=$seconds")

fun Response.sMaxAge(seconds: Int) = addCachingHeader("s-maxage=$seconds")


private fun Response.addCachingHeader(value: String) =
        if(header("Cache-Control").isNullOrEmpty()) header("Cache-Control", value) else extendCachingHeader(value)

private fun Response.extendCachingHeader(additionalValue: String) =
        replaceHeader("Cache-Control", "${header("Cache-Control")}, $additionalValue")

