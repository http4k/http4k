package org.http4k.client

import org.http4k.core.Status

fun Status.toClientStatus(e: Exception) = description(
    "Client Error: $description" + (
    e.localizedMessage?.let { " caused by $it" } ?: ""))
