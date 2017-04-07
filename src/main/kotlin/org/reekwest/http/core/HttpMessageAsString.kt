package org.reekwest.http.core

import org.reekwest.http.core.entity.StringEntity


fun Request.toStringMessage(): String = listOf("$method $uri $version", headers.toMessage(), StringEntity.fromEntity(entity)).joinToString("\r\n")

fun Response.toStringMessage(): String = listOf("$version $status", headers.toMessage(), org.reekwest.http.core.entity.StringEntity.fromEntity(entity)).joinToString("\r\n")

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")

private val version = "HTTP/1.1"
