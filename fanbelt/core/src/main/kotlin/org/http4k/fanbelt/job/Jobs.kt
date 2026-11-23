/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.SubscriberFailed
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.infrastructure.at
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs the jobs it is given on this node: each key an operation names, as it is named, again when this node starts and once per heal,
 * under a lock so only one node runs a key at a time. Add it to the app's Node.
 */
class Jobs(private val infra: AppInfra, vararg jobs: Job<*>) : Lifecycle {
    private val jobs = jobs.toList().also { jobs ->
        val shared = jobs.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(shared.isEmpty()) { "Jobs share a name, so would run each other's work: ${shared.joinToString()}" }
    }.associateBy { it.name }
    private val scanning = AtomicBoolean(false)
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private var listening = emptyList<Subscription>()

    @Volatile
    private var closed = false

    override fun start() {
        listening = jobs.keys.map { name -> infra.bus.listen(JobTopic.interest(name), ::heard, ::scan) }
        wakeRecorded()
    }

    override fun close() {
        closed = true
        listening.forEach(Subscription::close)
    }

    private fun heard(placed: Topic) {
        val topic = JobTopic.parse(placed.value)
        reporting { infra.storage.read { JobTables.owing(it, topic) }.forEach { wake(it, topic) } }
    }

    private fun scan() {
        if (scanning.compareAndSet(false, true)) reporting(::rescan)
    }

    private fun rescan() {
        scanning.set(false)
        wakeRecorded()
    }

    private fun reporting(block: () -> Unit) =
        infra.runtime.scheduler.execute { runCatching(block).onFailure { infra.runtime.events(SubscriberFailed(it)) } }

    private fun wakeRecorded() = infra.storage.read(JobTables::recorded).forEach { (tenant, topic) -> wake(tenant, topic) }

    private fun wake(tenant: TenantId, topic: JobTopic) {
        val job = jobs[topic.job] ?: return
        if (!closed && pending.add("$topic/$tenant")) infra.runtime.scheduler.execute { attempt(job, tenant, topic) }
    }

    private fun attempt(job: Job<*>, tenant: TenantId, topic: JobTopic) {
        if (closed) return
        val name = "$topic/$tenant"
        var settled = true
        runCatching { infra.storage.locks(name) { settled = runUntilDone(job, tenant, topic) } }
            .onFailure { infra.runtime.events(SubscriberFailed(it)) }
        if (settled) pending.remove(name) else infra.runtime.at(infra.runtime.clock.instant() + RETRY) { attempt(job, tenant, topic) }
    }

    private fun runUntilDone(job: Job<*>, tenant: TenantId, topic: JobTopic): Boolean {
        while (!closed) {
            val named = infra.storage.read { JobTables(it.tables(tenant)).named(topic) } ?: return true
            if (!job.runIn(infra.scope(tenant), topic)) return false
            if (infra.storage.write { JobTables(it.tables(tenant)).done(topic, named) }) return true
        }
        return true
    }
}

private val RETRY = Duration.ofSeconds(1)
