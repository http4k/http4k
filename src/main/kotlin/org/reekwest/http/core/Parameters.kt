package org.reekwest.http.core

import java.net.URLDecoder

fun Request.query(name: String): String? = uri.queries().findSingle(name)

fun Request.queries(name: String): List<String?> = uri.queries().findMultiple(name)

private fun Uri.queries(): Parameters = query.toParameters()

internal fun String.toParameters() = split("&").map(String::toParameter)

internal fun Parameters.findSingle(name: String): String? = find { it.first == name }?.second

internal fun Parameters.findMultiple(name: String) = filter { it.first == name }.map { it.second }

private fun String.toParameter(): Parameter = split("=").map(String::decode).let { l -> l.elementAt(0) to l.elementAtOrNull(1) }

private fun String.decode() = URLDecoder.decode(this, "UTF-8")

typealias Parameters = List<Parameter>

private typealias Parameter = Pair<String, String?>