import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.lambda.FakeLambda
import org.http4k.connect.amazon.lambda.Http
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.lambda.action.invokeFunction
import org.http4k.connect.amazon.lambda.model.FunctionName
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import org.http4k.format.Moshi
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader

data class Req(val value: String)
data class Resp(val value: String)

const val USE_REAL_CLIENT = false

fun main() {
    val deployedLambda = FunctionName.of("http4kLambda")

    val fakeLambda = FakeLambda(
        FnLoader {
            FnHandler { e: ScheduledEvent, ctx: Context ->
                println(e.toString())
                e.toString()
            }
        }
    )

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else fakeLambda

    // create a client
    val client = Lambda.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val invokeResult: Result<Resp, RemoteFailure> = client.invokeFunction(
        deployedLambda,
        ScheduledEvent().apply {
            account = "awsAccount"
        }, Moshi
    )
    println(invokeResult)
}

