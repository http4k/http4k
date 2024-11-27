import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import dev.forkhandles.mock4k.mock
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.AwsLambdaMoshi
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.AwsLambdaEventFunction
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.serverless.ServerlessFilters.ReportFnTransaction
import org.http4k.serverless.then
import java.io.ByteArrayOutputStream

// This is the handler for the incoming AWS SQS event. It's just a function so you can call it without any infrastructure
fun EventFnHandler(http: HttpHandler) =
    FnHandler { e: SQSEvent, _: Context ->
        e.records.forEach {
            http(Request(POST, "http://localhost:8080/").body(it.body.reversed()))
        }
        "processed ${e.records.size} messages"
    }

// We can add filters to the FnHandler if we want to - in this case print the transaction (with the letency).
val loggingFunction = ReportFnTransaction<SQSEvent, Context, String> { tx ->
    println(tx)
}

// The FnLoader is responsible for constructing the handler and for handling the serialisation of the request and response
fun EventFnLoader(http: HttpHandler) = FnLoader { env: Map<String, String> ->
    loggingFunction.then(EventFnHandler(http))
}

// This class is the entry-point for the Lambda function call - configure it when deploying
class EventFunction : AwsLambdaEventFunction(EventFnLoader(JavaHttpClient()))

fun main() {
    // this server receives the reversed event
    val receivingServer = { req: Request ->
        println(req.bodyString())
        Response(OK)
    }.asServer(SunHttp(8080)).start()

    val sqsEvent = SQSEvent().apply {
        records = listOf(
            SQSMessage().apply { body = "hello world" },
            SQSMessage().apply { body = "goodbye world" }
        )
    }

    fun runLambdaInMemoryOrForTesting() {
        println("RUNNING In memory:")
        val app = EventFnHandler(JavaHttpClient())
        app(sqsEvent, mock())
    }

    fun runLambdaAsAwsWould() {
        println("RUNNING as AWS would invoke the function:")

        val out = ByteArrayOutputStream()

        EventFunction().handleRequest(AwsLambdaMoshi.asInputStream(sqsEvent), out, mock())

        // the response is empty b
        println(out.toString())
    }

    runLambdaInMemoryOrForTesting()
    runLambdaAsAwsWould()

    receivingServer.stop()
}
