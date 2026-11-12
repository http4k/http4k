package helpdesk

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import helpdesk.domain.Org
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.Tickets
import helpdesk.feature.seed
import helpdesk.store.HelpdeskJson
import helpdesk.store.HelpdeskTables
import helpdesk.store.Orgs
import org.http4k.config.Environment.Companion.ENV
import io.lettuce.core.RedisClient
import org.http4k.config.EnvironmentKey
import org.http4k.core.Uri
import org.http4k.lens.uri
import org.http4k.fanbelt.bus.redis.RedisTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.redis.RedisPresenceBackend
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.SecureRandom
import java.time.Clock

val POSTGRES_URL = EnvironmentKey.uri().required("POSTGRES_URL")

val REDIS_URL = EnvironmentKey.uri().required("REDIS_URL")

fun main() {
    val provider = FakeEmailProvider()
    val runtime = AppRuntime(http = provider)
    val redis = RedisClient.create(REDIS_URL(ENV).toString())
    val storage = ExposedDocumentStorage(localDatabase(POSTGRES_URL(ENV)), HelpdeskJson, PostgresDocuments)
    demo(storage, runtime.clock)
    val server = App(runtime, { runtime ->
        AppInfra(runtime, storage, RedisTransport(redis, runtime.events), RedisPresenceBackend(redis, runtime.clock, HelpdeskJson), ENV)
    }, provider).asServer(Jetty(0)).start()
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(); redis.shutdown() })
    println("Fanbelt helpdesk on http://localhost:${server.port()}")
}

private fun demo(storage: DocumentStorage, clock: Clock) {
    val demo = TenantId.of("demo")
    Orgs(storage).create(demo, Org(clock.instant()))
    storage.write { Tickets(HelpdeskTables(it.tables(demo)), SecureRandom()).seed() }
}

private fun localDatabase(url: Uri) = Database.connect(HikariDataSource(HikariConfig().apply { jdbcUrl = url.toString() })).apply {
    transaction(this) {
        SchemaUtils.create(*listOf(
            HelpdeskTables.ORGS,
            HelpdeskTables.TICKETS,
            HelpdeskTables.REPLIES,
            HelpdeskTables.JOURNAL,
            HelpdeskTables.AGENTS,
            HelpdeskTables.OUTBOX
        ).map(PostgresDocuments::schema).toTypedArray())
    }
}