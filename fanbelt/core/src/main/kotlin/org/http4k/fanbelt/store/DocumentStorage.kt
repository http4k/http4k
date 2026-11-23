package org.http4k.fanbelt.store

/**
 * A database of tenant documents. A write transaction may touch only one tenant; work that conflicts is retried, so may
 * run more than once. Its locks are shared by every node using the same database.
 */
interface DocumentStorage {
    val locks: Locks

    fun <R> write(work: (Tenants) -> R): R

    fun <R> read(work: (Tenants) -> R): R = write(work)
}
