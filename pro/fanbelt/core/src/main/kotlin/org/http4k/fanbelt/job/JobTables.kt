/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.JobTopic
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.everything
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Instant

internal data class JobRow(override val id: String, val named: Int = 1, val retryAt: Instant? = null) : Identified<String>

/**
 * The keys jobs still owe, one row per job topic in each tenant's fanbelt_jobs table, counting how often it was named.
 */
internal class JobTables(tables: Tables) {
    private val rows = tables(JOBS, JobRow::class)

    fun record(topics: Set<Topic>) = topics
        .filterIsInstance<JobTopic>()
        .forEach { topic ->
            if (rows.get(topic.value) == null) rows.insert(JobRow(topic.value))
            else rows.update(topic.value) { it.copy(named = it.named + 1) }
        }

    fun named(topic: JobTopic) = rows.get(topic.value)?.named

    fun done(topic: JobTopic, named: Int?): Boolean {
        val row = rows.get(topic.value) ?: return true
        if (row.named != named) return false
        rows.delete(topic.value)
        return true
    }

    fun retry(topic: JobTopic, at: Instant) = rows.update(topic.value) { it.copy(retryAt = at) }

    fun recorded() = rows.page().items

    companion object {
        fun recorded(tenants: Tenants): List<Pair<TenantId, JobRow>> =
            tenants.having(JOBS, everything<JobRow>()).flatMap { tenant -> JobTables(tenants.tables(tenant)).recorded().map { tenant to it } }
    }
}

private val JOBS = TableName.of("fanbelt_jobs")
