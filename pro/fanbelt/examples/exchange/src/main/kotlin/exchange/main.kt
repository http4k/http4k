package exchange

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exchange.domain.VenueDetails
import exchange.fakes.FakeFeed
import exchange.store.ExchangeJson
import exchange.store.ExchangeTables
import exchange.store.Venues
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.lens.int
import org.http4k.lens.string
import io.lettuce.core.RedisClient
import org.http4k.lens.uri
import org.http4k.fanbelt.bus.redis.RedisTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.redis.RedisPresenceBackend
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.store.sqlite.SqliteDatabase
import org.http4k.fanbelt.store.sqlite.SqliteDocumentStorage
import org.http4k.fanbelt.store.sqlite.SqliteDocuments
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Helidon
import org.http4k.server.Jetty
import org.http4k.server.JettyLoom
import org.http4k.server.PolyServerConfig
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.time.Clock

val SQLITE_FILE = EnvironmentKey.string().defaulted("SQLITE_FILE", "exchange.db")

val POSTGRES_URL = EnvironmentKey.uri().optional("POSTGRES_URL")

val REDIS_URL = EnvironmentKey.uri().defaulted("REDIS_URL", Uri.of("redis://localhost:6379"))

val NODES = EnvironmentKey.int().defaulted("NODES", 3)

val SERVER = EnvironmentKey.string().defaulted("SERVER", "jetty")

private fun server(port: Int): PolyServerConfig = when (SERVER(ENV)) {
    "jettyloom" -> JettyLoom(port)
    "helidon" -> Helidon(port)
    "undertow" -> Undertow(port)
    else -> Jetty(port)
}

fun main() {
    val feed = FakeFeed(Clock.systemUTC()).asServer(Jetty(9100)).start()
    val newStorage = freshStorage()
    val redis = RedisClient.create(REDIS_URL(ENV).toString())
    val nodes = (0 until NODES(ENV)).map { node ->
        val storage = newStorage()
        val runtime = AppRuntime(http = SetBaseUriFrom(Uri.of("http://localhost:${feed.port()}")).then(JavaHttpClient()))
        if (node == 0) Venues(storage).create(TenantId.of("demo"), VenueDetails(runtime.clock.instant()))
        App(runtime, { AppInfra(it, storage, RedisTransport(redis, it.events), RedisPresenceBackend(redis, it.clock, ExchangeJson), ENV) })
            .asServer(server(9000 + node)).start()
    }
    Runtime.getRuntime().addShutdownHook(Thread { nodes.forEach { it.stop() }; redis.shutdown(); feed.stop() })
    nodes.forEach { println("Fanbelt exchange node on http://localhost:${it.port()} (${SERVER(ENV)})") }
    println("Price feed on http://localhost:${feed.port()}/prices")
}

private fun freshStorage(): () -> DocumentStorage {
    val postgres = POSTGRES_URL(ENV)
    if (postgres != null) {
        val tables = ExchangeTables.all.map(PostgresDocuments::schema).toTypedArray()
        transaction(Database.connect(postgres.toString())) {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
        return {
            val db = Database.connect(HikariDataSource(HikariConfig().apply { jdbcUrl = postgres.toString() }))
            ExposedDocumentStorage(db, ExchangeJson, PostgresDocuments)
        }
    }
    val file = File(SQLITE_FILE(ENV))
    listOf("", "-wal", "-shm").forEach { File(file.path + it).delete() }
    transaction(SqliteDatabase(file)) { SchemaUtils.create(*ExchangeTables.all.map(SqliteDocuments::schema).toTypedArray()) }
    return { SqliteDocumentStorage(file, ExchangeJson) }
}