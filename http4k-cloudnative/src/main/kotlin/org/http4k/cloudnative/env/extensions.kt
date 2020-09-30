package org.http4k.cloudnative.env

import org.http4k.format.JacksonYaml
import java.io.File

/**
 * Read a YAML file into environments, making
 */
fun Environment.Companion.fromYaml(file: File): Environment {
    val map = JacksonYaml.asA<Map<String, Any>>(file.reader().use { it.readText() })

    fun Map<*, *>.flatten(): List<Pair<String, String>> {
        fun convert(key: Any?, value: Any?): List<Pair<String, String>> {
            val keyString = (key ?: "").toString()

            return when (value) {
                is List<*> -> listOf(keyString to value.flatMap { convert(null, it) }.map { it.second }.joinToString(","))
                is Map<*, *> -> value.flatten().map { "$keyString.${it.first}" to it.second }
                else -> listOf((keyString to (value ?: "").toString()))
            }
        }

        return entries.fold(listOf()) { acc, (key, value) -> acc + convert(key, value) }
    }

    return MapEnvironment.from(map.flatten().toMap().toProperties())
}
