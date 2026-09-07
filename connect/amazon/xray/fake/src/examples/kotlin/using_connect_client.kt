import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.xray.FakeXRay
import org.http4k.connect.amazon.xray.Http
import org.http4k.connect.amazon.xray.XRay
import org.http4k.connect.amazon.xray.action.TraceSummaries
import org.http4k.connect.amazon.xray.batchGetTraces
import org.http4k.connect.amazon.xray.getTraceSummaries
import org.http4k.connect.model.Timestamp
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import java.time.Instant

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeXRay()

    // create a client
    val client = XRay.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val now = Instant.now()

    // all operations return a Result monad of the API type
    val summaries: Result<TraceSummaries, RemoteFailure> = client.getTraceSummaries(
        StartTime = Timestamp.of(now.minusSeconds(300)),
        EndTime = Timestamp.of(now),
        FilterExpression = """annotation.order_id = "my-order"""",
    )
    println(summaries)

    summaries.map { println(client.batchGetTraces(it.TraceSummaries.take(5).map { summary -> summary.Id })) }
}
