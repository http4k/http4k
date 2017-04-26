package org.reekwest.http.core

import org.reekwest.http.core.body.bodyString

fun Request.toStringMessage(): String = listOf("$method $uri $version", headers.string(), bodyString()).joinToString("\r\n")

fun Response.toStringMessage(): String = listOf("$version $status", headers.string(), bodyString()).joinToString("\r\n")

private fun Headers.string() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")

private val version = "HTTP/1.1"
