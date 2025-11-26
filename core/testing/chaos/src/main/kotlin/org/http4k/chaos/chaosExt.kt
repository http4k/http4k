package org.http4k.chaos

import org.http4k.format.Jackson.asA
import tools.jackson.databind.JsonNode

internal inline fun <reified T : Any> JsonNode.asNullable(name: String): T? = if (hasNonNull(name)) this[name].asA() else null
internal inline fun <reified T : Any> JsonNode.nonNullable(name: String): T = this[name].asA()
