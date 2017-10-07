package org.http4k.core

import java.net.URLDecoder
import java.net.URLEncoder

typealias Parameters = List<Parameter>

fun Uri.queries(): Parameters = query.toParameters()

fun Parameters.toUrlEncoded(): String = this.joinToString("&") { it.first.encode() + it.second?.let { "=" + it.encode() }.orEmpty() }

fun String.toParameters() = if(isNotEmpty()) split("&").map(String::toParameter) else listOf()

internal fun Parameters.findSingle(name: String): String? = find { it.first == name }?.second

internal fun Parameters.findMultiple(name: String) = filter { it.first == name }.map { it.second }

private fun String.toParameter(): Parameter = split("=").map(String::decode).let { l -> l.elementAt(0) to l.elementAtOrNull(1) }

internal fun String.decode() = URLDecoder.decode(this, "UTF-8")

internal fun String.encode() = URLEncoder.encode(this, "UTF-8")

private typealias Parameter = Pair<String, String?>