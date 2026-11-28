package org.http4k.fanbelt.store

/**
 * Locks shared by every node.
 */
fun interface Locks {
    operator fun invoke(name: String, block: () -> Unit): Boolean
}
