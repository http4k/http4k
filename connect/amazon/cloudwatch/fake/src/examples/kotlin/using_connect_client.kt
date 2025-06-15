import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.cloudwatch.CloudWatch
import org.http4k.connect.amazon.cloudwatch.FakeCloudWatch
import org.http4k.connect.amazon.cloudwatch.Http
import org.http4k.connect.amazon.cloudwatch.action.Metrics
import org.http4k.connect.amazon.cloudwatch.listMetrics
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeCloudWatch()

    // create a client
    val cloudWatch =
        CloudWatch.Http(Region.US_EAST_1, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val result: Result<Metrics, RemoteFailure> = cloudWatch.listMetrics(
        Namespace = Namespace.of("foobar"),
    )

    println(result)
}
