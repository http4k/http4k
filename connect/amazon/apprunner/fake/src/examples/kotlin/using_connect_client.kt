import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.apprunner.AppRunner
import org.http4k.connect.amazon.apprunner.FakeAppRunner
import org.http4k.connect.amazon.apprunner.Http
import org.http4k.connect.amazon.apprunner.listServices
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

data class Req(val value: String)
data class Resp(val value: String)

const val USE_REAL_CLIENT = false

fun main() {
    val fakeAppRunner = FakeAppRunner()

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else fakeAppRunner

    // create a client
    val client = AppRunner.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    println(client.listServices())
}

