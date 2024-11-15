import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.cloudwatchlogs.CloudWatchLogs
import org.http4k.connect.amazon.cloudwatchlogs.FakeCloudWatchLogs
import org.http4k.connect.amazon.cloudwatchlogs.Http
import org.http4k.connect.amazon.cloudwatchlogs.action.PutLogEventsResponse
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import org.http4k.connect.amazon.cloudwatchlogs.putLogEvents
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeCloudWatchLogs()

    // create a client
    val cloudWatchLogs =
        CloudWatchLogs.Http(Region.US_EAST_1, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val result: Result<PutLogEventsResponse, RemoteFailure> = cloudWatchLogs.putLogEvents(
        LogGroupName.of("foobar"),
        LogStreamName.of("stream"),
        emptyList()
    )

    println(result)
}
