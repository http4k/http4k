package org.http4k.cloudnative.env

import java.nio.charset.StandardCharsets

/**
 * A secret is a value which tries very hard not to expose itself as a string, by storing it's value in a byte array. It is also able to be cleared after construction.
 */
data class Secret(val value: ByteArray) {
    constructor(value: String) : this(value.toByteArray(StandardCharsets.UTF_8))

    override fun equals(other: Any?): Boolean = value.contentEquals((other as Secret).value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = "Secret(hashcode = ${hashCode()})"

    fun stringValue(): String = value.toString(Charsets.UTF_8)

    fun clear() = apply { (0 until value.size).forEach { value[it] = 0 } }
}