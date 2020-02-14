package guide.modules.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.AppLoader
import org.http4k.serverless.BootstrapAppLoader
import org.http4k.serverless.lambda.LambdaFunction

// This AppLoader is responsible for building our HttpHandler which is supplied to AWS
// It is the only actual piece of code that needs to be written.
object TweetEchoLambda : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = {
        Response(OK).body(it.bodyString().take(20))
    }
}

fun main() {

    // Launching your Lambda Function locally - by simply providing the operating ENVIRONMENT map as would
    // be configured on AWS.
    fun runLambdaLocally() {
        val app: HttpHandler = TweetEchoLambda(mapOf())
        val localLambda = app.asServer(SunHttp(8000)).start()

        println(ApacheClient()(Request(GET, "http://localhost:8000/").body("hello hello hello, i suppose this isn't 140 characters anymore..")))
        localLambda.stop()
    }

    // the following code is purely here for demonstration purposes, to explain exactly what is happening at AWS.
    fun runLambdaAsAwsWould() {
        val lambda = LambdaFunction(mapOf(BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS to TweetEchoLambda::class.java.name))
        val response = lambda.handle(APIGatewayProxyRequestEvent().apply {
            path = "/"
            body = "hello hello hello, i suppose this isn't 140 characters anymore.."
            httpMethod = "GET"
            headers = mapOf()
            queryStringParameters = mapOf()
        })
        println(response.statusCode)
        println(response.headers)
        println(response.body)
    }

    runLambdaLocally()
    runLambdaAsAwsWould()

}
