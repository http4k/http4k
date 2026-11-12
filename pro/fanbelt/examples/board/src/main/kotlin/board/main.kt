package board

import board.domain.Team
import board.fakes.FakeCi
import board.store.BoardJson
import board.store.BoardTables
import board.store.Teams
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.core.Uri
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.fanbelt.bus.postgres.PostgresTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.exposed.ExposedPresenceBackend
import org.http4k.fanbelt.presence.exposed.PresenceTable
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val POSTGRES_URL = EnvironmentKey.uri().required("POSTGRES_URL")

val CI_TOKEN = EnvironmentKey.string().defaulted("CI_TOKEN", "local-ci")

fun main() {
    val runtime = AppRuntime()
    val dataSource = localDatabase(POSTGRES_URL(ENV))
    val db = Database.connect(dataSource)
    val storage = ExposedDocumentStorage(db, BoardJson, PostgresDocuments)
    Teams(storage).create(TenantId.of("demo"), Team(runtime.clock.instant()))
    val token = CI_TOKEN(ENV)
    val server = App(runtime, { runtime ->
        AppInfra(
            runtime,
            storage,
            PostgresTransport(dataSource::getConnection, runtime.scheduler, runtime.events),
            ExposedPresenceBackend(db, runtime.clock, BoardJson),
            ENV,
        )
    }, token).asServer(Jetty(0)).start()
    val ci = FakeCi("http://localhost:${server.port()}/teams/demo/b/lobby/builds", token)
    Runtime.getRuntime().addShutdownHook(Thread {
        ci.close()
        server.stop()
    })
    println("Fanbelt board on http://localhost:${server.port()}, with a fake CI reporting builds to /teams/demo/b/lobby")
}

private fun localDatabase(url: Uri) = HikariDataSource(HikariConfig().apply { jdbcUrl = url.toString() }).apply {
    transaction(Database.connect(this)) { SchemaUtils.create(*(listOf(
        BoardTables.CARDS,
        BoardTables.MEMBERS,
        BoardTables.TEAMS,
        BoardTables.BUILDS
    ).map(PostgresDocuments::schema) + PresenceTable()).toTypedArray()) }
}