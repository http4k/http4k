package org.http4k.cloudnative.env

data class Host(val value: String) {
    init {
        if (value.isEmpty()) throw IllegalArgumentException("Could not construct Host from '$value'")
    }

    companion object {
        val localhost = Host("localhost")
    }
}