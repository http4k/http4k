package org.http4k.fanbelt.store

/**
 * A database of tenant documents.
 */
interface DocumentStorage {
    val locks: Locks

    fun <R> write(work: (Tenants) -> R): R

    fun <R> read(work: (Tenants) -> R): R = write(work)
}
