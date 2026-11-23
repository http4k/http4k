/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.fanbelt.bus.postgres.PostgresTransport
import org.http4k.fanbelt.eventually
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.postgres.PostgresDocumentStorage
import org.http4k.fanbelt.store.postgres.postgresUrl
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class PostgresJobsTest {
    class OrderId private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<OrderId>(::OrderId)
    }

    private object JobsJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().done())

    private class Orders(name: JobName) : Feature<Unit, Unit>({}) {
        val ran = CopyOnWriteArrayList<String>()
        val charge by job(OrderId, name = name) { scope, id -> ran += "$id in ${scope.tenant}" }
        val checkout by operation { _, _, id: String -> Changes.of(charge.topic(OrderId.of(id))) }
    }

    private val app = "jobs-${System.nanoTime()}"
    private val db by lazy {
        Database.connect("$postgresUrl&ApplicationName=$app").also { transaction(it) { SchemaUtils.create(PostgresDocumentStorage.schema(TableName.of("fanbelt_jobs"))) } }
    }
    private val scheduler = Executors.newScheduledThreadPool(8)
    private val running = CopyOnWriteArrayList<Lifecycle>()
    private val acme = TenantId.of("acme-${System.nanoTime()}")
    private val globex = TenantId.of("globex-${System.nanoTime()}")
    private val orders = Orders(JobName.of("charge-${System.nanoTime()}"))

    @AfterEach
    fun stop() {
        running.reversed().forEach { runCatching { it.close() } }
        scheduler.shutdownNow()
    }

    @Test
    fun `a key named on a web node runs on a worker node`() {
        val web = node()
        runs(Jobs(node(), orders.charge))

        web.scope(acme).execute(orders.checkout, Unit, "42")

        eventually { orders.ran.isNotEmpty() }
        assertThat(settled(), equalTo(listOf("42 in $acme")))
    }

    @Test
    fun `the same key named in two tenants runs once in each`() {
        val web = node()
        runs(Jobs(node(), orders.charge))

        web.scope(acme).execute(orders.checkout, Unit, "42")
        web.scope(globex).execute(orders.checkout, Unit, "42")

        eventually { orders.ran.size == 2 }
        assertThat(settled().sorted(), equalTo(listOf("42 in $acme", "42 in $globex").sorted()))
    }

    @Test
    fun `a key recorded before any worker starts runs when one does`() {
        node().scope(acme).execute(orders.checkout, Unit, "42")

        runs(Jobs(node(), orders.charge))

        eventually { orders.ran.isNotEmpty() }
        assertThat(settled(), equalTo(listOf("42 in $acme")))
    }

    @Test
    fun `two workers run a key once`() {
        val web = node()
        runs(Jobs(node(), orders.charge))
        runs(Jobs(node(), orders.charge))

        web.scope(acme).execute(orders.checkout, Unit, "42")

        eventually { orders.ran.isNotEmpty() }
        assertThat(settled(), equalTo(listOf("42 in $acme")))
    }

    private fun node() = AppInfra(
        AppRuntime(scheduler = scheduler, events = {}),
        PostgresDocumentStorage(db, JobsJson),
        PostgresTransport({ DriverManager.getConnection("$postgresUrl&ApplicationName=$app") }, scheduler, {}),
    ).also(::runs)

    private fun runs(lifecycle: Lifecycle) {
        lifecycle.start()
        running += lifecycle
    }

    private fun settled(): List<String> {
        Thread.sleep(500)
        return orders.ran.toList()
    }
}
