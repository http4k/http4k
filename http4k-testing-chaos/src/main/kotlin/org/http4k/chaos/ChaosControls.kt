package org.http4k.chaos

import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Variable
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.NoOp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.json
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Clock.systemUTC

/**
 * Adds a set of endpoints to an application which will control the switching on/off of chaos behaviour. The added endpoints are:
 *  GET /<control path>/status <- check the on off/status of the injected chaos
 *  POST /<control path>/activate <- turn on the chaos. optionally POST a JSON body to set a list of new stages to use.
 *  POST /<control path>/deactivate <- turn off the chaos
 *  POST /<control path>/toggle <- toggle the chaos
 *
 *  By default, controls are mounted at the root path /chaos
 */
object ChaosControls {
    private val setStages = Body.json().map { json ->
        (if (json.isArray) json else listOf(json))
                .map { json.asStage(Clock.systemUTC()) }
                .reduce { acc, next -> acc.then(next) }
    }.toLens()

    operator fun invoke(trigger: SwitchTrigger, variable: Variable, controlsPath: String = "/chaos"): RoutingHttpHandler {
        fun response() = Response(OK).body("chaos active: ${if (trigger.isActive()) variable.toString() else "none"}")

        return routes(
                controlsPath bind
                        routes(
                                "/status" bind GET to { response() },
                                "/activate" bind POST to {
                                    if (it.body.stream.available() != 0) variable.current = setStages(it)
                                    trigger.toggle(true)
                                    response()
                                },
                                "/set" bind POST to {
                                    trigger.toggle()
                                    response()
                                },
                                "/deactivate" bind POST to {
                                    trigger.toggle(false)
                                    response()
                                },
                                "/toggle" bind POST to {
                                    trigger.toggle()
                                    response()
                                }
                        )
        )
    }
}

/**
 * Convert a standard HttpHandler to be Chaos-enabled, using the passed ChaosStage.
 * Optionally a Filter can be passed to limit access to the chaos controls.
 */
fun HttpHandler.withChaosControls(stage: ChaosStage,
                                  preChaosFilter: Filter = Filter.NoOp,
                                  controlsPath: String = "/chaos",
                                  clock: Clock = systemUTC()): RoutingHttpHandler {
    val trigger = SwitchTrigger()
    val variable = Variable(stage)
    val repeatStage = Repeat { Wait.until(trigger).then(variable).until(!trigger) }
    return routes(ChaosControls(trigger, variable, controlsPath).withFilter(preChaosFilter), "/" bind repeatStage.asFilter(clock).then(this))
}
