package org.http4k.connect.amazon

import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.Region

fun interface RegionProvider : () -> Region? {

    fun orElseThrow() = requireNotNull(invoke()) { "AWS Region not found in provider chain" }
    infix fun orElse(other: RegionProvider) = RegionProvider { invoke() ?: other() }

    companion object
}

fun RegionProvider.Companion.Environment(env: Environment) = RegionProvider { AWS_REGION_OPTIONAL(env) }

fun RegionProvider.Companion.Environment(env: Map<String, String> = System.getenv()) =
    Environment(Environment.from(env))
