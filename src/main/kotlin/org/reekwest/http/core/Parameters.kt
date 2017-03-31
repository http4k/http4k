package org.reekwest.http.core

import java.net.URLDecoder
import java.net.URLEncoder

typealias Parameters = List<Parameter>

fun Request.query(name: String): String? = uri.queries().findSingle(name)

fun Request.queries(name: String): List<String?> = uri.queries().findMultiple(name)

fun HttpMessage.header(name: String): String? = headers.find { it.first.equals(name, true) }?.second

fun HttpMessage.headerValues(name: String): List<String?> = headers.filter { it.first.equals(name, true) }.map { it.second }

private fun Uri.queries(): Parameters = query.toParameters()

fun Parameters.toUrlEncoded(): String = this.map { it.first.encode() + it.second?.let { "=" + it.encode() }.orEmpty() }.joinToString("&")

internal fun String.toParameters() = split("&").map(String::toParameter)

internal fun Parameters.findSingle(name: String): String? = find { it.first == name }?.second

internal fun Parameters.findMultiple(name: String) = filter { it.first == name }.map { it.second }

private fun String.toParameter(): Parameter = split("=").map(String::decode).let { l -> l.elementAt(0) to l.elementAtOrNull(1) }

private fun String.decode() = URLDecoder.decode(this, "UTF-8")

private fun String.encode() = URLEncoder.encode(this, "UTF-8")

private typealias Parameter = Pair<String, String?>