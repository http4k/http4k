package org.http4k.config

data class Port(val value: Int) {
    init {
        require(value <= 65535) { "Out of range Port: '$value'" }
    }

    companion object {
        val RANDOM = Port(0)
    }
}
