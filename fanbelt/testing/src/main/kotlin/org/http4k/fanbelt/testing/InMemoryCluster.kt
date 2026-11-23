package org.http4k.fanbelt.testing

import org.http4k.config.Environment
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import java.time.Duration

/**
 * Several in-process nodes sharing storage (in memory unless given), presence and a broker. Build each app with App(cluster.runtime,
 * cluster.node()).
 */
class InMemoryCluster(val runtime: TestRuntime = TestRuntime(), val storage: DocumentStorage = InMemoryDocumentStorage()) {
    private val presenceBackend = InMemoryPresenceBackend(runtime.clock)
    private val broker = InMemoryBroker()

    fun node(): (AppRuntime) -> AppInfra = { runtime -> AppInfra(runtime, storage, broker.transport(), presenceBackend, Environment.EMPTY) }

    fun tick(by: Duration) {
        runtime.scheduler.tick(by)
        runtime.scheduler.runUntilIdle()
    }
}

/**
 * Closes a node and everything it runs, its jobs included, as if it died.
 */
fun Node.dies() = close()
