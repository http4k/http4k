package org.http4k.chaos

import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Variable
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.contract.ApiInfo
import org.http4k.contract.NoSecurity
import org.http4k.contract.OpenApi
import org.http4k.contract.Security
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.format.Jackson.obj
import org.http4k.format.Jackson.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

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
    operator fun invoke(
            trigger: SwitchTrigger,
            variable: Variable,
            controlsPath: String = "/chaos",
            security: Security = NoSecurity,
            openApiPath: String = "",
            corsPolicy: CorsPolicy = UnsafeGlobalPermissive

    ): RoutingHttpHandler {
        val setStages = Body.json().map { node ->
            (if (node.isArray) node.elements().asSequence() else sequenceOf(node))
                    .map { it.asStage(Clock.systemUTC()) }
                    .reduce { acc, next -> acc.then(next) }
        }.toLens()

        val showCurrentStatus: HttpHandler = {
            Response(OK).with(Body.json().toLens() of obj(
                    "chaos" to string(if (trigger.isActive()) variable.toString() else "none")
            ))
        }

        val activate = Filter { next ->
            {
                if (it.body.stream.available() != 0) variable.current = setStages(it)
                trigger.toggle(true)
                next(it)
            }
        }

        val deactivate = Filter { next ->
            {
                trigger.toggle(false)
                next(it)
            }
        }

        val toggle = Filter { next ->
            {
                trigger.toggle()
                next(it)
            }
        }
        val description = ""

        val exampleChaos = obj(
        )

        return controlsPath bind
                Cors(corsPolicy)
                        .then(
                                contract(OpenApi(ApiInfo("Http4k Chaos controls", "1.0", description), Jackson),
                                        openApiPath,
                                        security,
                                        "/status" meta {
                                            summary = "show the current chaos being applied"
                                        } bindContract GET to showCurrentStatus,
                                        "/activate/new" meta {
                                            summary = "activate new chaos on all routes"
                                            receiving(Body.json("New chaos to be applied").toLens() to exampleChaos)
                                        } bindContract POST to activate.then(showCurrentStatus),
                                        "/activate" meta {
                                            summary = "activate chaos on all routes"
                                        } bindContract POST to activate.then(showCurrentStatus),
                                        "/deactivate" meta {
                                            summary = "deactivate the chaos on all routes"
                                        } bindContract POST to deactivate.then(showCurrentStatus),
                                        "/toggle" meta {
                                            summary = "toggle the chaos on all routes"
                                        } bindContract POST to toggle.then(showCurrentStatus)
                                )
                        )
    }
}

/**
 * Convert a standard HttpHandler to be Chaos-enabled, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun HttpHandler.withChaosControls(stage: Stage = Wait,
                                  security: Security = NoSecurity,
                                  controlsPath: String = "/chaos",
                                  openApiPath: String = "",
                                  corsPolicy: CorsPolicy = UnsafeGlobalPermissive
): RoutingHttpHandler = routes("/{path:.*}" bind this).withChaosControls(stage, security, controlsPath, openApiPath, corsPolicy)

/**
 * Convert a standard HttpHandler to be Chaos-enabled, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun RoutingHttpHandler.withChaosControls(stage: Stage = Wait,
                                         security: Security = NoSecurity,
                                         controlsPath: String = "/chaos",
                                         openApiPath: String = "",
                                         corsPolicy: CorsPolicy = UnsafeGlobalPermissive
): RoutingHttpHandler {
    val trigger = SwitchTrigger()
    val variable = Variable(stage)
    val repeatStage = Repeat { Wait.until(trigger).then(variable).until(!trigger) }
    return routes(ChaosControls(trigger, variable, controlsPath, security, openApiPath, corsPolicy), repeatStage.asFilter().then(this))
}
