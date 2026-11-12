/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import org.http4k.config.Environment
import org.http4k.fanbelt.bus.Backplane
import org.http4k.fanbelt.bus.Bus
import org.http4k.fanbelt.bus.LocalTransport
import org.http4k.fanbelt.bus.Transport
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_BACKPLANE_HEAL
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_PRESENCE_HEARTBEAT
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_PRESENCE_TTL
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_REDRAW_LIMIT
import org.http4k.fanbelt.infrastructure.FanbeltSettings.FANBELT_STREAM_KEEPALIVE
import org.http4k.fanbelt.presence.Presence
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.SharedPresence
import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import java.util.concurrent.Semaphore

class AppInfra(
    internal val runtime: AppRuntime,
    val storage: DocumentStorage = InMemoryDocumentStorage(),
    transport: Transport = LocalTransport,
    presenceBackend: PresenceBackend = InMemoryPresenceBackend(runtime.clock),
    env: Environment = Environment.EMPTY,
) : Lifecycle {
    internal val bus: Bus = Backplane(transport, runtime.scheduler, runtime.random, runtime.events, FANBELT_BACKPLANE_HEAL(env))
    internal val presence: Presence = SharedPresence(presenceBackend, bus, runtime.scheduler, runtime.random, runtime.events, FANBELT_PRESENCE_TTL(env), FANBELT_PRESENCE_HEARTBEAT(env))

    internal val keepAlive = FANBELT_STREAM_KEEPALIVE(env)

    private val redrawing = Semaphore(FANBELT_REDRAW_LIMIT(env).value, true)

    fun scope(tenant: TenantId) = Scope(tenant, storage, bus, presence, redrawing, runtime)

    override fun start() {
        bus.start()
        presence.start()
    }

    override fun close() {
        listOf(presence, bus).forEach { runCatching { it.close() } }
    }
}
