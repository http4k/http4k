import dev.forkhandles.result4k.Result
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.FakeIot
import org.http4k.connect.amazon.iot.Http
import org.http4k.connect.amazon.iot.Iot
import org.http4k.connect.amazon.iot.action.CreatedJob
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iot.describeJob
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    val region = Region.of("us-east-1")

    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeIot()

    // create a client
    val client = Iot.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val jobId = JobId.of("firmware-update-1")

    // all operations return a Result monad of the API type
    val created: Result<CreatedJob, RemoteFailure> = client.createJob(
        jobId = jobId,
        targets = listOf(ARN.of("arn:aws:iot:us-east-1:000000000000:thing/my-thing")),
        document = """{"operation":"firmware-update","url":"https://example.com/firmware.bin"}""",
        description = "Firmware update to 1.2.3",
    )
    println(created)

    println(client.describeJob(jobId))
}
