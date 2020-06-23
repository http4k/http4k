package org.http4k.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class CliFlags(val args: Array<String>)

class CLIFlag private constructor(private val default: String? = null) : ReadOnlyProperty<CliFlags, String> {
    override fun getValue(thisRef: CliFlags, property: KProperty<*>): String {
        val windowed = thisRef.args.toList().windowed(2).map { it[0] to it[1] }.toMap()
        return windowed["--${property.name}"] ?: default
        ?: throw IllegalArgumentException("no --${property.name} passed")
    }

    companion object {
        fun required() = CLIFlag()
        fun defaulted(default: String?) = CLIFlag(default)
    }
}
