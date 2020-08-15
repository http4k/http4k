package guide.modules.serverless.lambda

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.AppLoader
import org.http4k.serverless.LambdaFunction

// This AppLoader is responsible for building our HttpHandler which is supplied to AWS
// It is the only actual piece of code that needs to be written.
object TweetEchoLambda : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = {
        Response(OK).body(it.bodyString().take(17))
    }
}

// This class is the entry-point for the function call - configure it when deploying
class FunctionsExampleEntryClass : LambdaFunction(TweetEchoLambda)

fun main() {
    // Launching your Lambda Function locally - by simply providing the operating ENVIRONMENT map as would
    // be configured on AWS.
    fun runLambdaLocally() {
        val app: HttpHandler = TweetEchoLambda(mapOf())
        val localLambda = app.asServer(SunHttp(8000)).start()

        val response = ApacheClient()(Request(POST, "http://localhost:8000/").body("hello hello hello, i suppose this isn't 140 characters anymore.."))

        println(response)
        localLambda.stop()
    }

    // the following code is purely here for demonstration purposes, to explain exactly what is happening at AWS.
    fun runLambdaAsAwsWould() {
        val response = FunctionsExampleEntryClass().handle(APIGatewayProxyRequestEvent().apply {
            path = "/"
            body = "hello hello hello, i suppose this isn't 140 characters anymore.."
            httpMethod = "GET"
            headers = mapOf()
            queryStringParameters = mapOf()
        })
        println(response)
    }

    runLambdaLocally()
    runLambdaAsAwsWould()
}
