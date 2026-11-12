package org.http4k.fanbelt.load

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.lettuce.core.RedisClient
import org.http4k.fanbelt.bus.Transport
import org.http4k.fanbelt.bus.memory.InMemoryBroker
import org.http4k.fanbelt.bus.postgres.PostgresTransport
import org.http4k.fanbelt.bus.redis.RedisTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.presence.PresenceBackend
import org.http4k.fanbelt.presence.exposed.ExposedPresenceBackend
import org.http4k.fanbelt.presence.exposed.PresenceTable
import org.http4k.fanbelt.presence.memory.InMemoryPresenceBackend
import org.http4k.fanbelt.presence.redis.RedisPresenceBackend
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.template.HandlebarsTemplates
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import sheets.App
import sheets.domain.SheetDetails
import sheets.store.SheetJson
import sheets.store.SheetTables
import sheets.store.Sheets
import java.time.Clock
import java.time.Instant

val POSTGRES: String? = System.getenv("FANBELT_TEST_PG")
val REDIS: String? = System.getenv("FANBELT_TEST_REDIS")

fun mode() = listOfNotNull(
    if (POSTGRES == null) "memory storage" else "Postgres storage",
    when {
        REDIS != null -> "Redis backplane and presence"
        POSTGRES != null -> "Postgres backplane and presence"
        else -> "in-memory backplane and presence"
    },
).joinToString(", ")

class SheetsCluster(size: Int, private val counts: Counts) : AutoCloseable {
    val tenant = TenantId.of("load-${System.nanoTime()}")
    private val postgres = if (POSTGRES == null) null else postgres(POSTGRES)
    private val storage = CountingStorage(if (postgres == null) InMemoryDocumentStorage() else ExposedDocumentStorage(postgres.db, SheetJson, PostgresDocuments), counts)
    private val redis = if (REDIS == null) null else RedisClient.create(REDIS)
    private val broker = InMemoryBroker()
    private val sharedPresence = InMemoryPresenceBackend(Clock.systemUTC())
    private val renderer = counting(HandlebarsTemplates().CachingClasspath(), counts)
    private val infras = mutableListOf<AppInfra>()

    val nodes: List<Node> = (1..size).map { App(AppRuntime(), { runtime -> node(runtime).also(infras::add) }, renderer) }

    init {
        nodes.forEach(Node::start)
        Sheets(storage).create(tenant, SheetDetails(Instant.now()))
    }

    fun scope(node: Int) = infras[node].scope(tenant)

    override fun close() {
        nodes.forEach(Node::close)
        redis?.shutdown()
        postgres?.dataSource?.close()
    }

    private fun node(runtime: AppRuntime) = AppInfra(
        runtime,
        storage,
        CountingTransport(transport(runtime), counts),
        CountingPresence(presence(), counts),
    )

    private fun transport(runtime: AppRuntime): Transport = when {
        redis != null -> RedisTransport(redis, runtime.events)
        postgres != null -> PostgresTransport(postgres.dataSource::getConnection, runtime.scheduler, runtime.events)
        else -> broker.transport()
    }

    private fun presence(): PresenceBackend = when {
        redis != null -> RedisPresenceBackend(redis, Clock.systemUTC(), SheetJson)
        postgres != null -> ExposedPresenceBackend(postgres.db, Clock.systemUTC(), SheetJson)
        else -> sharedPresence
    }
}

class Postgres(val dataSource: HikariDataSource, val db: Database)

fun postgres(url: String): Postgres {
    val dataSource = HikariDataSource(HikariConfig().apply { jdbcUrl = url; maximumPoolSize = 20 })
    val db = Database.connect(dataSource)
    transaction(db) {
        SchemaUtils.create(*(SheetTables.all.map(PostgresDocuments::schema) + PresenceTable()).toTypedArray())
    }
    return Postgres(dataSource, db)
}

