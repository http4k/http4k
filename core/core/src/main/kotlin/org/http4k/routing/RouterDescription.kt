package org.http4k.routing

data class RouterDescription(val description: String, val children: List<RouterDescription> = listOf()) {
    override fun toString(): String = friendlyToString()

    companion object {
        val unavailable = RouterDescription("unavailable")
    }
}

fun RouterDescription.friendlyToString(indent: Int = 0): String = "$description\n" + children.joinToString("") {
    "\t".repeat(indent + 1) + it.friendlyToString(indent + 1)
}
