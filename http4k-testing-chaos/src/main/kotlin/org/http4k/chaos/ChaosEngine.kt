package org.http4k.chaos

import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.contract.security.NoSecurity
import org.http4k.contract.security.Security
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.CorsPolicy
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Convert a standard HttpHandler to be Chaos-enabled, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun HttpHandler.withChaosEngine(stage: Stage = Wait,
                                security: Security = NoSecurity,
                                controlsPath: String = "/chaos",
                                openApiPath: String = "",
                                corsPolicy: CorsPolicy = CorsPolicy.UnsafeGlobalPermissive
): RoutingHttpHandler = routes("/{path:.*}" bind this).withChaosEngine(stage, security, controlsPath, openApiPath, corsPolicy)

/**
 * Convert a standard HttpHandler to be Chaos-enabled, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun RoutingHttpHandler.withChaosEngine(stage: Stage = Wait,
                                       security: Security = NoSecurity,
                                       controlsPath: String = "/chaos",
                                       openApiPath: String = "",
                                       corsPolicy: CorsPolicy = CorsPolicy.UnsafeGlobalPermissive
): RoutingHttpHandler {
    val engine = ChaosEngine(stage, false)
    return routes(
        RemoteChaosApi(engine, controlsPath, security, openApiPath, corsPolicy),
        engine.then(this)
    )
}

class ChaosEngine(initialStage: Stage = Wait, initialPosition: Boolean = true) : Filter {
    private val on = AtomicBoolean(initialPosition)
    private val trigger: Trigger = { on.get() }
    private val state = ChaosStages.Variable(initialStage)

    override fun invoke(p1: HttpHandler) = Repeat { Wait.until(trigger).then(state).until(!trigger) }.asFilter()(p1)

    override fun toString() = state.toString()

    fun isActive() = on.get()
    fun toggle(newValue: Boolean? = null) = on.set(newValue ?: !on.get())

    fun update(stage: Stage) {
        state.current = stage
    }

    fun update(behaviour: Behaviour) {
        state.current = behaviour.appliedWhen(trigger)
    }
}
