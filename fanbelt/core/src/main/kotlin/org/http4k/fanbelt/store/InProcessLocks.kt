package org.http4k.fanbelt.store

import java.util.concurrent.ConcurrentHashMap

/**
 * Locks held in this process: enough for one node, or for storage only one process uses.
 */
class InProcessLocks : Locks {
    private val held = ConcurrentHashMap.newKeySet<String>()

    override fun invoke(name: String, block: () -> Unit): Boolean {
        if (!held.add(name)) return false
        try {
            block()
        } finally {
            held.remove(name)
        }
        return true
    }
}
