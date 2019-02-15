package org.http4k.cloudnative.env

import java.time.Duration

data class Port(val value: Int)

data class Host(val value: String) {
    init {
        if (value.isEmpty()) throw IllegalArgumentException("Could not construct Host from '$value'")
    }

    companion object {
        val localhost = Host("localhost")
    }
}

data class Authority(val host: Host, val port: Port? = null) {
    override fun toString() = host.value + (port?.let { ":${it.value}" } ?: "")

    companion object {
        operator fun invoke(value: String) = with(value.split(":")) {
            when (size) {
                1 -> Authority(Host(this[0]), null)
                2 -> Authority(Host(this[0]), Port(this[1].toInt()))
                else -> throw IllegalArgumentException("Could not construct Authority from $value")
            }
        }
    }
}

/**
 * A secret is a value which tries very hard not to expose itself as a string
 */
data class Secret(private val bytes: ByteArray) {
    fun stringValue() = String(bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Secret

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int = bytes.contentHashCode()
}

data class Timeout(val value: Duration)