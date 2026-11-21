/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.SubscriberFailed
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.infrastructure.at
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs the features' jobs on this node: each key an operation names, as it is named and again whenever this node starts
 * or reconnects, under a lock so only one node runs a key at a time. Add it to the app's Node.
 */
class Jobs(private val infra: AppInfra, vararg features: Feature<*, *>) : Lifecycle {
    private val jobs = features.flatMap { it.jobs }.also { jobs ->
        val shared = jobs.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(shared.isEmpty()) { "Jobs share a name, so would run each other's work: ${shared.joinToString()}" }
    }.associateBy { it.name }
    private val running = ConcurrentHashMap<Topic, Boolean>()
    private val scanning = AtomicBoolean(false)
    private var listening = emptyList<Subscription>()

    @Volatile
    private var closed = false

    override fun start() {
        listening = jobs.keys.map { name ->
            infra.bus.listen(JobTopic.interest(name), ::heard) {
                if (scanning.compareAndSet(false, true)) infra.runtime.scheduler.execute(::rescan)
            }
        }
        wakeRecorded()
    }

    override fun close() {
        closed = true
        listening.forEach(Subscription::close)
    }

    private fun heard(placed: Topic) {
        val (tenant, topic) = JobTopic.heard(placed)
        wake(tenant, topic)
    }

    private fun rescan() {
        scanning.set(false)
        wakeRecorded()
    }

    private fun wakeRecorded() = infra.storage.read(JobTables::recorded).forEach { (tenant, row) ->
        val topic = JobTopic.parse(row.id)
        if (row.retryAt != null) retry(tenant, topic, row.retryAt) else wake(tenant, topic)
    }

    private fun wake(tenant: TenantId, topic: JobTopic) {
        if (closed) return
        val job = jobs[topic.job] ?: return
        val placed = tenant(topic)
        if (running.put(placed, true) == null) infra.runtime.scheduler.execute { drain(job, tenant, topic, placed) }
    }

    private fun drain(job: Job<*, *>, tenant: TenantId, topic: JobTopic, placed: Topic) {
        do {
            while (!closed && running.replace(placed, true, false)) {
                runCatching { if (!infra.storage.locks(placed.value) { run(job, tenant, topic) }) retry(tenant, topic, infra.runtime.clock.instant() + RETRY) }
                    .onFailure { infra.runtime.events(SubscriberFailed(it)) }
            }
        } while (!closed && !running.remove(placed, false))
    }

    private fun run(job: Job<*, *>, tenant: TenantId, topic: JobTopic) {
        val named = infra.storage.read { JobTables(it.tables(tenant)).named(topic) } ?: return
        if (job.runIn(infra.scope(tenant), topic)) {
            if (!infra.storage.write { JobTables(it.tables(tenant)).done(topic, named) }) running[tenant(topic)] = true
            return
        }
        val at = infra.runtime.clock.instant() + RETRY
        infra.storage.write { JobTables(it.tables(tenant)).retry(topic, at) }
        retry(tenant, topic, at)
    }

    private fun retry(tenant: TenantId, topic: JobTopic, at: Instant) {
        infra.runtime.at(at) { wake(tenant, topic) }
    }
}

private val RETRY = Duration.ofSeconds(1)
