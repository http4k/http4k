package guide.tutorials.going_native_with_graal_on_aws_lambda

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.serverless.ApiGatewayV1FnLoader
import org.http4k.serverless.AwsLambdaRuntime
import org.http4k.serverless.asServer

val http4kApp = routes(
    "/echo/{message:.*}" bind GET to {
        Response(OK).body(
            it.path("message") ?: "(nothing to echo, use /echo/<message>)"
        )
    },
    "/" bind GET to { Response(OK).body("ok") }
)

fun main() {
    ApiGatewayV1FnLoader(http4kApp).asServer(AwsLambdaRuntime()).start()
}
