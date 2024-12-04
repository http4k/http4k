package org.http4k.routing

data class RouterDescription(val description: String) {

    override fun toString() = description

    companion object {
        val unavailable = RouterDescription("unavailable")
    }
}
