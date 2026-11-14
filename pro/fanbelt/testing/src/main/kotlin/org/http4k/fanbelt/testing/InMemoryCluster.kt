/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.config.Environment
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import java.time.Duration

/**
 * Several nodes in one process, sharing in-memory storage, presence and a broker, on one `TestRuntime`. Build each
 * app with `App(cluster.runtime, cluster.node())` to run a contract with users spread across nodes.
 */
class InMemoryCluster(val runtime: TestRuntime = TestRuntime()) {
    val storage = InMemoryDocumentStorage()
    private val presenceBackend = InMemoryPresenceBackend(runtime.clock)
    private val broker = InMemoryBroker()
    private val built = mutableListOf<AppInfra>()
    val nodes: List<AppInfra> get() = built.toList()

    fun node(): (AppRuntime) -> AppInfra = { runtime -> AppInfra(runtime, storage, broker.transport(), presenceBackend, Environment.EMPTY).also(built::add) }

    fun tick(by: Duration) {
        runtime.scheduler.tick(by)
        runtime.scheduler.runUntilIdle()
    }
}

/**
 * Kills a node, taking its viewers with it, to test what the surviving nodes do.
 */
fun AppInfra.dies() = close()
