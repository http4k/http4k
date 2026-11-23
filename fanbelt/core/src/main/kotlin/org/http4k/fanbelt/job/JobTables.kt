/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId

internal data class JobRow(override val id: Topic, val job: String, val key: String, val named: Int = 1) : Identified<Topic>

/**
 * The keys jobs still owe, one row per job topic in each tenant's fanbelt_jobs table, counting how often it was named.
 */
internal class JobTables(tables: Tables) {
    private val rows = tables(JOBS, JobRow::class)

    fun record(topics: Set<Topic>) = topics
        .filterIsInstance<JobTopic>()
        .forEach { topic ->
            if (rows.get(topic.row) == null) {
                rows.insert(JobRow(topic.row, topic.job, topic.key))
            } else {
                rows.update(topic.row) { it.copy(named = it.named + 1) }
            }
        }

    fun named(topic: JobTopic) = rows.get(topic.row)?.named

    fun done(topic: JobTopic, named: Int): Boolean {
        val row = rows.get(topic.row) ?: return true
        if (row.named != named) return false
        rows.delete(topic.row)
        return true
    }

    fun recorded() = rows.page().items

    companion object {
        fun recorded(tenants: Tenants): List<Pair<TenantId, JobTopic>> =
            tenants.having(JOBS, everything<JobRow>()).flatMap { tenant -> JobTables(tenants.tables(tenant)).recorded().map { tenant to JobTopic.of(it.job, it.key) } }

        fun owing(tenants: Tenants, topic: JobTopic) = tenants.having(JOBS, (JobRow::job eq topic.job) and (JobRow::key eq topic.key))
    }
}

private val JOBS = TableName.of("fanbelt_jobs")

private val JobTopic.row get() = Topic.of(value)
