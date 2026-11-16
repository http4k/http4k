/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.JobTopic
import org.http4k.fanbelt.bus.Subscriber
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.bus.job
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.infrastructure.at
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

/**
 * Runs the features' jobs on this node: each key an operation names, as it is named and again whenever this node starts
 * or reconnects, under a lock so only one node runs a key at a time. Add it to the app's Node.
 */
class Jobs(private val infra: AppInfra, vararg features: Feature<*, *>) : Lifecycle {
    private val jobs = features.flatMap { it.jobs }.also { jobs ->
        val shared = jobs.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(shared.isEmpty()) { "Jobs share a name, so would run each other's work: ${shared.joinToString()}" }
    }.associateBy { it.name }
    private val runs = ConcurrentHashMap<Topic, Subscriber>()
    private val retries = ConcurrentHashMap<Topic, ScheduledFuture<*>>()
    private var listening = emptyList<Subscription>()

    override fun start() {
        listening = jobs.keys.map { name ->
            infra.bus.listen(Interest.of(Topic.job(name)), { heard -> wake(TenantId.ofJob(heard), JobTopic(heard.value.substringBeforeLast('/'))) }) {
                infra.runtime.scheduler.execute(::wakeRecorded)
            }
        }
        wakeRecorded()
    }

    override fun close() {
        listening.forEach(Subscription::close)
        retries.values.forEach { it.cancel(false) }
    }

    private fun wakeRecorded() = infra.storage.read(JobTables::recorded).forEach { (tenant, row) ->
        val topic = JobTopic(row.id)
        if (row.retryAt != null) retry(tenant, topic, row.retryAt) else wake(tenant, topic)
    }

    private fun wake(tenant: TenantId, topic: JobTopic) {
        val job = jobs[topic.value.removePrefix("@").substringBefore('/')] ?: return
        runs.computeIfAbsent(tenant(topic)) { placed ->
            Subscriber(infra.runtime.scheduler, infra.runtime.events) {
                infra.locks.tryLock(placed.value)?.use { run(job, tenant, topic) }
                    ?: retry(tenant, topic, infra.runtime.clock.instant() + RETRY)
            }
        }.markDirty()
    }

    private fun run(job: Job<*, *>, tenant: TenantId, topic: JobTopic) {
        if (job.runIn(infra.scope(tenant), topic)) return infra.storage.write { JobTables(it.tables(tenant)).forget(topic) }
        val at = infra.runtime.clock.instant() + RETRY
        infra.storage.write { JobTables(it.tables(tenant)).retry(topic, at) }
        retry(tenant, topic, at)
    }

    private fun retry(tenant: TenantId, topic: JobTopic, at: Instant) {
        retries.put(tenant(topic), infra.runtime.at(at) { wake(tenant, topic) })?.cancel(false)
    }
}

private val RETRY = Duration.ofSeconds(1)
