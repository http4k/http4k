package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.Http
import org.http4k.connect.amazon.iot.Iot
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iot.deleteJob
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.successValue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.lang.System.getenv
import org.http4k.connect.amazon.iot.model.JobId as IotJobId

/**
 * Jobs are seeded through the real control plane, so a real thing has to be supplied. Set
 * HTTP4K_IOT_THING_ARN to the ARN of an existing thing WITH NO PENDING JOB EXECUTIONS to
 * run this against a real account.
 */
class RealIotJobsDataPlaneTest : IotJobsDataPlaneContract, RealAwsContract {

    private val thingArn: ARN
        get() = getenv("HTTP4K_IOT_THING_ARN")
            .also { assumeTrue(it != null, "HTTP4K_IOT_THING_ARN not set") }
            .let(ARN::of)

    override val thingName get() = ThingName.of(thingArn.value.substringAfterLast(":").substringAfterLast("/"))

    private val iot get() = Iot.Http(aws.region, { aws.credentials }, http)

    /** Every case is gated, including any which never read [thingArn] themselves. */
    @BeforeEach
    fun assumeRealThingConfigured() {
        thingArn
    }

    override fun createJob(jobId: JobId, document: String) {
        iot.createJob(IotJobId.of(jobId.value), listOf(thingArn), document).successValue()
    }

    override fun cleanupJob(jobId: JobId) {
        iot.deleteJob(IotJobId.of(jobId.value), force = true)
    }
}
