package org.http4k.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosStages.Repeat
import org.http4k.chaos.ChaosStages.Wait
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.contract.RouteMetaDsl
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.NoSecurity
import org.http4k.contract.security.Security
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.Cors
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.format.Jackson.obj
import org.http4k.format.Jackson.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.Instant.ofEpochSecond

/**
 * Mixin the set of remote Chaos API endpoints to a standard HttpHandler, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun HttpHandler.withChaosApi(engine: ChaosEngine = ChaosEngine(),
                             security: Security = NoSecurity,
                             controlsPath: String = "/chaos",
                             openApiPath: String = "",
                             corsPolicy: CorsPolicy = UnsafeGlobalPermissive,
                             clock: Clock = Clock.systemUTC()
) = routes("/{path:.*}" bind this).withChaosApi(engine, security, controlsPath, openApiPath, corsPolicy, clock)

/**
 * Mixin the set of remote Chaos API endpoints to a standard HttpHandler, using the passed ChaosStage.
 * Optionally a Security can be passed to limit access to the chaos controls.
 */
fun RoutingHttpHandler.withChaosApi(engine: ChaosEngine = ChaosEngine(),
                                    security: Security = NoSecurity,
                                    controlsPath: String = "/chaos",
                                    openApiPath: String = "",
                                    corsPolicy: CorsPolicy = UnsafeGlobalPermissive,
                                    clock: Clock = Clock.systemUTC()
) = routes(
    RemoteChaosApi(engine, controlsPath, security, openApiPath, corsPolicy, clock),
    engine.then(this)
)

/**
 *  A set of endpoints to an application which will control the setting and toggling chaos behaviour. The added endpoints are:
 *  GET /<control path>/status <- check the on off/status of the injected chaos
 *  POST /<control path>/activate <- turn on the chaos. optionally POST a JSON body to set a list of new stages to use.
 *  POST /<control path>/deactivate <- turn off the chaos
 *  POST /<control path>/toggle <- toggle the chaos
 *
 *  By default, controls are mounted at the root path /chaos
 */
object RemoteChaosApi {
    operator fun invoke(
        engine: ChaosEngine,
        controlsPath: String = "/chaos",
        chaosSecurity: Security = NoSecurity,
        openApiPath: String = "",
        corsPolicy: CorsPolicy = UnsafeGlobalPermissive,
        clock: Clock = Clock.systemUTC()
    ): RoutingHttpHandler {
        val setStages = Body.json().map { node ->
            (if (node.isArray) node.elements().asSequence() else sequenceOf(node))
                .map { it.asStage(clock) }
                .reduce { acc, next -> acc.then(next) }
        }.toLens()

        val jsonLens = Body.json().toLens()
        val showCurrentStatus: HttpHandler = {
            Response(OK).with(jsonLens of chaosStatus(if (engine.isEnabled()) engine.toString() else "none"))
        }

        val activate = Filter { next ->
            {
                if (it.bodyString().isNotEmpty()) engine.enable(setStages(it))
                else engine.enable()
                next(it)
            }
        }

        val deactivate = Filter { next ->
            {
                engine.disable()
                next(it)
            }
        }

        val toggle = Filter { next ->
            {
                with(engine) {
                    if (isEnabled()) disable() else enable()
                }
                next(it)
            }
        }
        val apiDescription = """This is the Open API interface for the http4k Chaos Engine. 
            |
            |Using this UI you can inject new dynamic chaotic behaviour into any http4k application, or toggle/disable it. 
            |
            |See the <a href="https://www.http4k.org/guide/modules/chaos/">user guide</a> for details about the 
            | exact format of the JSON to post to the activation endpoint.""".trimMargin()


        val currentChaosDescription = Repeat {
            Wait.until(Delay(ofMinutes(1), clock))
                .then(ReturnStatus(I_M_A_TEAPOT)
                    .appliedWhen(Always())
                    .until(Deadline(ofEpochSecond(1735689600))))
        }.toString()

        fun RouteMetaDsl.returningExampleChaosDescription() =
            returning(OK, jsonLens to chaosStatus(currentChaosDescription), "The current Chaos being applied to requests.")

        return controlsPath bind
            Cors(corsPolicy)
                .then(ServerFilters.CatchAll())
                .then(
                    contract {
                        renderer = OpenApi3(ApiInfo("http4k Chaos Engine", "1.0", apiDescription))
                        descriptionPath = openApiPath
                        security = chaosSecurity
                        routes += "/status" meta {
                            summary = "Show the current Chaos being applied."
                            description = "Returns a textual description of the current Chaos behaviour being applied to traffic."
                            returningExampleChaosDescription()
                        } bindContract GET to showCurrentStatus
                        routes += "/activate/new" meta {
                            summary = "Activate new Chaos on all routes."
                            description = "Replace the current Chaos being applied to traffic and activates that behaviour."
                            receiving(jsonLens to exampleChaos)
                            returning(BAD_REQUEST to "New Chaos could not be deserialised from the request body.")
                            returningExampleChaosDescription()
                        } bindContract POST to activate.then(showCurrentStatus)
                        routes += "/activate" meta {
                            summary = "Activate Chaos on all routes."
                            description = "Toggles on the previously stored Chaos behaviour."
                            returningExampleChaosDescription()
                        } bindContract POST to activate.then(showCurrentStatus)
                        routes += "/deactivate" meta {
                            summary = "Deactivate Chaos on all routes."
                            description = "Toggles off the previously stored Chaos behaviour."
                            returningExampleChaosDescription()
                        } bindContract POST to deactivate.then(showCurrentStatus)
                        routes += "/toggle" meta {
                            summary = "Toggle on/off the Chaos on all routes."
                            description = "Toggles the previously stored Chaos behaviour."
                            returningExampleChaosDescription()
                        } bindContract POST to toggle.then(showCurrentStatus)
                    }
                )
    }
}

private fun chaosStatus(value: String) = obj("chaos" to string(value))

private val exampleChaos = Jackson {
    array(listOf(
        obj("type" to string("repeat"),
            "stages" to array(
                listOf(
                    obj("type" to string("wait"),
                        "until" to obj("type" to string("delay"), "period" to string("PT30S"))
                    ),
                    obj("type" to string("trigger"),
                        "behaviour" to obj("type" to string("status"), "status" to number(418)),
                        "trigger" to obj("type" to string("always")),
                        "until" to obj("type" to string("countdown"), "count" to number(10))
                    )
                )
            ),
            "until" to obj("type" to string("deadline"),
                "endTime" to string("2030-01-01T00:00:00Z"))
        )
    ))
}
