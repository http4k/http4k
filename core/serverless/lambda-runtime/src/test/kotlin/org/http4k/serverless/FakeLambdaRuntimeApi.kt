package org.http4k.serverless

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.format.AwsLambdaMoshi.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.serverless.LambdaRuntimeAPI.Companion.deadline
import org.http4k.serverless.LambdaRuntimeAPI.Companion.lambdaArn
import org.http4k.serverless.LambdaRuntimeAPI.Companion.requestId
import org.http4k.serverless.LambdaRuntimeAPI.Companion.traceId
import java.time.Clock
import java.util.UUID
import java.util.UUID.randomUUID

class FakeLambdaRuntimeApi(
    private val clock: Clock = Clock.systemUTC(),
    private val random: () -> UUID = { randomUUID() }
) : HttpHandler {

    private val body = Body.auto<Any>().toLens()
    val events = mutableListOf<Any>()
    val responses = mutableListOf<String>()
    val errors = mutableListOf<String>()

    private val app = CatchAll()
        .then(
            routes(
                "/2018-06-01/runtime" bind routes(
                    "/invocation/next" bind GET to {
                        if (events.isEmpty()) Response(SERVICE_UNAVAILABLE)
                        else Response(OK)
                            .with(body of events.removeAt(0))
                            .with(requestId of random())
                            .with(traceId of random().toString())
                            .with(lambdaArn of "arn:aws:lambda:eu-west-2:1234567890:function:function")
                            .with(deadline of clock.instant().plusSeconds(900))
                    },
                    "/invocation/{id}/response" bind POST to {
                        responses.add(it.bodyString())
                        Response(ACCEPTED)
                    },
                    "/invocation/{id}/error" bind POST to {
                        errors.add(it.bodyString())
                        Response(ACCEPTED)
                    },
                    "/init/error" bind POST to {
                        errors.add(it.bodyString())
                        Response(ACCEPTED)
                    }
                )
            )
        )

    override fun invoke(p1: Request): Response = app(p1)
}
