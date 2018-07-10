package org.http4k.core

import java.net.URLDecoder
import java.net.URLEncoder

typealias Parameters = List<Parameter>

fun Uri.queries(): Parameters = query.toParameters()

@Deprecated("rename for clarity", ReplaceWith("this.toUrlFormEncoded()"))
fun Parameters.toUrlEncoded(): String = toUrlFormEncoded()

fun Parameters.toUrlFormEncoded(): String = this.joinToString("&") { it.first.toFormEncoded() + it.second?.let { "=" + it.toFormEncoded() }.orEmpty() }

fun Parameters.toParametersMap(): Map<String, List<String?>> = this.groupBy(Pair<String, String?>::first, Pair<String, String?>::second)

fun <K, V> Map<K, List<V>>.getFirst(key: K) = this[key]?.firstOrNull()

fun String.toParameters() = if (isNotEmpty()) split("&").map(String::toParameter) else listOf()

internal fun Parameters.findSingle(name: String): String? = find { it.first == name }?.second

internal fun Parameters.findMultiple(name: String) = filter { it.first == name }.map { it.second }

private fun String.toParameter(): Parameter = split("=").map(String::fromFormEncoded).let { l -> l.elementAt(0) to l.elementAtOrNull(1) }

internal fun String.fromFormEncoded() = URLDecoder.decode(this, "UTF-8")

internal fun String.toFormEncoded() = URLEncoder.encode(this, "UTF-8")

private typealias Parameter = Pair<String, String?>

