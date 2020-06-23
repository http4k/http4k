package org.http4k.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Nano CLI flag library. Uses the name of the property to load from the passed array of args, else fetch blows up.
 */
abstract class CliFlags(val args: Array<String>) {
    fun requiredFlag() = CLIFlag()
    fun defaultedFlag(default: String?) = CLIFlag(default)
}

/**
 * Delegate that loads the named flag from the comment line
 */
class CLIFlag internal constructor(private val default: String? = null) : ReadOnlyProperty<CliFlags, String> {
    override fun getValue(thisRef: CliFlags, property: KProperty<*>): String {
        val windowed = thisRef.args.toList().windowed(2).map { it[0] to it[1] }.toMap()
        return windowed["--${property.name}"] ?: default
        ?: throw IllegalArgumentException("no --${property.name} passed")
    }
}

/**
 * Useful method to encapsulate usage of command line flags
 */
fun <T : CliFlags> T.use(fn: T.() -> Unit): Unit = fn(this)
