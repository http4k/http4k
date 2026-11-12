package sl4k

import io.lettuce.core.RedisClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.lens.uri
import org.http4k.fanbelt.bus.redis.RedisTransport
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.presence.redis.RedisPresenceBackend
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import sl4k.domain.ChannelName
import sl4k.domain.GENERAL
import sl4k.store.Teams
import java.time.Clock

val REDIS_URL = EnvironmentKey.uri().required("REDIS_URL")

fun main(args: Array<String>) {
    val storage = InMemoryDocumentStorage()
    Teams(storage).create(TenantId.of("demo"), Clock.systemUTC(), GENERAL, ChannelName.of("random"))

    val redis = RedisClient.create(REDIS_URL(ENV).toString())
    val server = App {
        AppInfra(
            it, storage,
            RedisTransport(redis, it.events),
            RedisPresenceBackend(redis, it.clock),
            ENV,
        )
    }.asServer(Jetty(args.firstOrNull()?.toInt() ?: 9000)).start()
    Runtime.getRuntime().addShutdownHook(Thread { server.stop(); redis.shutdown() })
    println("sl4k on http://localhost:${server.port()}/teams/demo/")
}
