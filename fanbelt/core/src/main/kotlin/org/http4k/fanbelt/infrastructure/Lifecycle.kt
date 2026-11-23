package org.http4k.fanbelt.infrastructure

/**
 * Something with background work, started and closed by its Node.
 */
interface Lifecycle : AutoCloseable {
    fun start() {}

    override fun close() {}
}
