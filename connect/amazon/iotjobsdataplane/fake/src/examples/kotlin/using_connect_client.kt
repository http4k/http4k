import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.FakeIot
import org.http4k.connect.amazon.iot.StoredJob
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iotjobsdataplane.FakeIotJobsDataPlane
import org.http4k.connect.amazon.iotjobsdataplane.Http
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlane
import org.http4k.connect.amazon.iotjobsdataplane.describeJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.SUCCEEDED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.amazon.iotjobsdataplane.startNextPendingJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.updateJobExecution
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import org.http4k.connect.amazon.iot.model.JobId as IotJobId

fun main() {
    val region = Region.of("us-east-1")
    val thingName = ThingName.of("my-thing")

    // one shared store means the control plane and the device API see the same jobs state
    val store = Storage.InMemory<StoredJob>()

    // the cloud side creates a job through the (fake) control plane...
    FakeIot(store).client().createJob(
        jobId = IotJobId.of("firmware-update-1"),
        targets = listOf(ARN.of("arn:aws:iot:$region:000000000000:thing/${thingName.value}")),
        document = """{"operation":"firmware-update","url":"https://example.com/firmware.bin"}""",
    )

    // ...and the device walks it through the jobs data plane
    val http: HttpHandler = FakeIotJobsDataPlane(store)
    val device = IotJobsDataPlane.Http(region, { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // $next is read-only: safe to poll on every connect
    println(device.describeJobExecution(thingName, JobId.NEXT))

    // claim it, then report the terminal status
    println(device.startNextPendingJobExecution(thingName, statusDetails = mapOf("step" to "downloading")))
    println(device.updateJobExecution(thingName, JobId.of("firmware-update-1"), SUCCEEDED))
}
