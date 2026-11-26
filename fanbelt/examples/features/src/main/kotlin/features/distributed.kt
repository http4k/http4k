package features

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import features.builds.BuildLog
import features.grid.GridTables
import features.notes.Noticeboard
import features.rooms.Rooms
import org.http4k.fanbelt.store.TableName
import io.lettuce.core.RedisClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.redis.RedisTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.presence.redis.RedisPresenceBackend
import org.http4k.fanbelt.store.postgres.PostgresDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private const val POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=secret"

private const val REDIS_URL = "redis://localhost:6379"

fun main() {
    val runtime = AppRuntime()
    val redis = RedisClient.create(REDIS_URL)
    val storage = PostgresDocumentStorage(database(), FeaturesJson)
    Spaces(storage).create(TenantId.of("demo"), SpaceDetails(runtime.clock.instant()))
    val statusService = StandInStatusService().start()
    val server = App(runtime, { runtime ->
        AppInfra(runtime, storage, RedisTransport(redis, runtime.events), RedisPresenceBackend(redis, runtime.clock), ENV)
    }, statusService = statusService.uri()).asServer(Jetty(0)).start()
    val ci = StandInCi(Uri.of("http://localhost:${server.port()}/spaces/demo/"))
    Runtime.getRuntime().addShutdownHook(Thread { ci.close(); server.stop(); statusService.stop(); redis.shutdown() })
    println("Fanbelt features on http://localhost:${server.port()}, over Postgres and Redis")
}

private fun database() = Database.connect(HikariDataSource(HikariConfig().apply { jdbcUrl = POSTGRES_URL })).apply {
    transaction(this) {
        SchemaUtils.create(
            *listOf(SpaceTables.SPACES, GridTables.CELLS, BuildLog.BUILDS, Noticeboard.NOTES, Rooms.ROOMS, Rooms.LINES, Rooms.SEEN, TableName.of("fanbelt_jobs"))
                .map(PostgresDocumentStorage::schema).toTypedArray()
        )
    }
}
