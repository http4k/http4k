package org.http4k.cloudnative.env

data class Port(val value: Int) {
    init {
        if (value > 65535) throw IllegalArgumentException("Out of range Port: $value'")
    }

    companion object {
        val RANDOM = Port(0)
    }
}

