package org.http4k.fanbelt.store

/**
 * Locks shared by every node, taken without waiting: runs the block holding the name and gives true, or gives false
 * when anyone holds it. A node that dies lets go of its locks.
 */
fun interface Locks {
    operator fun invoke(name: String, block: () -> Unit): Boolean
}
