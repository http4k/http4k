package org.http4k.chaos

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.format.Jackson.asA

internal inline fun <reified T : Any> JsonNode.asNullable(name: String): T? = if (hasNonNull(name)) this[name].asA() else null
internal inline fun <reified T : Any> JsonNode.nonNullable(name: String): T = this[name].asA()
