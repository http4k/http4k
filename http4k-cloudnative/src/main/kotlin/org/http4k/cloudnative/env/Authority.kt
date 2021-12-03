package org.http4k.cloudnative.env

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
