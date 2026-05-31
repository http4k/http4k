package org.http4k.config

import java.io.Closeable
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * A secret is a value which tries very hard not to expose itself as a string, by storing it's value in a byte array.
 * You can "use" the value only once, after which the value is destroyed
 */
class Secret(input: ByteArray) : Closeable {
    constructor(value: String) : this(value.toByteArray(UTF_8))

    init {
        require(input.isNotEmpty()) { "Cannot create an empty secret" }
    }

    private val value = AtomicReference(input)

    override fun equals(other: Any?): Boolean =
        other is Secret && MessageDigest.isEqual(value.get() ?: ByteArray(0), other.value.get() ?: ByteArray(0))

    override fun hashCode(): Int = 0

    override fun toString(): String = "Secret(****)"

    fun <T> use(fn: (String) -> T) = with(value.get()) {
        if (isNotEmpty()) fn(toString(UTF_8))
        else throw IllegalStateException("Cannot read a secret more than once")
    }.apply { close() }

    override fun close(): Unit = run {
        value.get().apply { indices.forEach { this[it] = 0 } }
        value.set(ByteArray(0))
    }
}
