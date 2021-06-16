package org.http4k.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.lang.Integer.MAX_VALUE
import java.util.Random
import kotlin.reflect.KClass

/**
 * Useful for creating HttpHandlers with a built-in Chaos Engine API for enabling and disabling chaos.
 */
abstract class ChaoticHttpHandler : HttpHandler {

    protected abstract val app: HttpHandler

    private val chaosEngine = ChaosEngine()

    fun behave() = chaosEngine.disable()

    fun misbehave(behaviour: Behaviour = ReturnStatus(INTERNAL_SERVER_ERROR)) = chaosEngine.enable(behaviour)

    fun misbehave(stage: Stage) = chaosEngine.enable(stage)

    fun returnStatus(status: Status) = misbehave(ReturnStatus(status))

    override fun invoke(request: Request) = chaosEngine
        .then(CatchAll())
        .then(app.withChaosApi(chaosEngine))(request)
}

/**
 * Convert this ChaoticHttpHandler into a running server, defaulting on a port dependent on the classname.
 */
fun ChaoticHttpHandler.start(
    port: Int = this::class.defaultPort, serverConfig: (Int) -> ServerConfig = ::SunHttp
) =
    asServer(serverConfig(port)).start().also {
        println("Started ${this::class.simpleName} on $port")
    }

/**
 * Calculate a random standard port number for a ChaoticHttpHandler using the classname as a seed
 */
val <T : ChaoticHttpHandler> KClass<T>.defaultPort
    get() = Random((simpleName.hashCode() % MAX_VALUE).toLong()).nextInt(65535 - 10000) + 10000

/**
 * Local URI for this ChaoticHttpHandler
 */
val <T : ChaoticHttpHandler> KClass<T>.defaultLocalUri get() = Uri.of("http://localhost:$defaultPort")
