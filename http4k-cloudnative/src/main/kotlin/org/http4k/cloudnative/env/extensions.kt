package org.http4k.cloudnative.env

import org.http4k.format.JacksonYaml
import java.io.File
import java.util.Properties

/**
 * Read a YAML file into environments, prepending all of the nested levels into the property names
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

fun Environment.Companion.fromConfigFile(file: File): Environment {
    val (_, named) = file.reader().readLines().foldRight(emptyList<String>() to mapOf<String, String>()) { next, (running, done) ->
        if (next.startsWith("[")) {
            val key = next.trim('[', ']').replace(' ', '.')
            emptyList<String>() to done + Properties().apply { load(running.joinToString("\n").reader()) }.toMap().map { key + "." + it.key to it.value.toString() }.toMap()
        } else {
            (running + next) to done
        }
    }
    return MapEnvironment.from(named.toProperties())
}
