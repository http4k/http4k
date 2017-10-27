package org.http4k.core

fun Response.noCache() = removeCachingHeaderIfSet().header("Cache-Control", "no-cache")

fun Response.private() = removeCachingHeaderIfSet().header("Cache-Control", "private")

fun Response.public() = removeCachingHeaderIfSet().header("Cache-Control", "public")

fun Response.onlyIfCached() = removeCachingHeaderIfSet().header("Cache-Control", "only-if-cached")

private fun Response.removeCachingHeaderIfSet(): Response = if(header("Cache-Control") != null) removeHeader("Cache-Control") else this
