package org.http4k.routing

import org.http4k.core.Request

interface Router {
    fun match(request: Request): RouterMatch

    val description: RouterDescription get() = RouterDescription.unavailable
}

data class RouterDescription(val description: String, val children: List<RouterDescription> = listOf()) {

    override fun toString(): String = friendlyToString()

    companion object {
        val unavailable = RouterDescription("unavailable")
    }
}

fun RouterDescription.friendlyToString(indent: Int = 0): String = "$description\n" + children.joinToString("") {
    "\t".repeat(indent + 1) + it.friendlyToString(indent + 1)
}
