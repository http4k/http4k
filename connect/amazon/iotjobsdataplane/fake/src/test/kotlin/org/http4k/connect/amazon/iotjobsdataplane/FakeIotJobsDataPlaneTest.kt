package org.http4k.connect.amazon.iotjobsdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.FakeIot
import org.http4k.connect.amazon.iot.StoredJob
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iot.deleteJob
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.SUCCEEDED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import org.http4k.connect.amazon.iot.model.JobExecutionStatus as StoredExecutionStatus
import org.http4k.connect.amazon.iot.model.JobId as IotJobId
import org.http4k.connect.amazon.iot.model.JobStatus as StoredJobStatus
import org.http4k.connect.amazon.iot.model.ThingName as StoredThingName

class FakeIotJobsDataPlaneTest : IotJobsDataPlaneContract, FakeAwsContract {

    private val store = Storage.InMemory<StoredJob>()

    override val http = FakeIotJobsDataPlane(store)

    private val fakeIot = FakeIot(store)

    override val thingName = ThingName.of("my-thing")

    private val thingArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/${thingName.value}")

    override fun createJob(jobId: JobId, document: String) {
        fakeIot.client().createJob(IotJobId.of(jobId.value), listOf(thingArn), document).successValue()
    }

    override fun cleanupJob(jobId: JobId) {
        fakeIot.client().deleteJob(IotJobId.of(jobId.value), force = true)
    }

    /** The two fakes share one store, so a device-side update is visible in the stored record. */
    @Test
    fun `a device update is written through to the shared store`() {
        val jobId = jobId("shared")

        createJob(jobId, """{"operation":"noop"}""")

        iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()
        iotJobsDataPlane.updateJobExecution(thingName, jobId, SUCCEEDED, statusDetails = mapOf("firmware" to "confirmed"))
            .successValue()

        val stored = store[jobId.value]!!

        assertThat(stored.status, equalTo(StoredJobStatus.COMPLETED))
        assertThat(stored.completedAt != null, equalTo(true))

        val execution = stored.executions[StoredThingName.of(thingName.value)]!!
        assertThat(execution.status, equalTo(StoredExecutionStatus.SUCCEEDED))
        assertThat(execution.statusDetails, equalTo(mapOf("firmware" to "confirmed")))
        assertThat(execution.versionNumber, equalTo(3L))
    }

    /** `$next` serves the oldest QUEUED execution first, so jobs are handed out in creation order. */
    @Test
    fun `next serves the oldest queued execution first`() {
        val first = jobId("first")
        val second = jobId("second")

        createJob(first, """{"n":1}""")
        createJob(second, """{"n":2}""")

        val picked = iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution!!

        assertThat(picked.jobId, equalTo(first))
    }

    /** An IN_PROGRESS execution takes precedence over a QUEUED one, whatever their ages. */
    @Test
    fun `next prefers an in-progress execution over a queued one`() {
        val first = jobId("first")
        val second = jobId("second")

        createJob(first, """{"n":1}""")
        createJob(second, """{"n":2}""")

        iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()

        val picked = iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution!!

        assertThat(picked.jobId, equalTo(first))

        iotJobsDataPlane.updateJobExecution(thingName, first, SUCCEEDED).successValue()

        assertThat(
            iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution!!.jobId,
            equalTo(second)
        )
    }

    /** The in-progress timeout budget is derived from the job's timeout config; no live timers. */
    @Test
    fun `an in-progress execution of a job with a timeout reports its full budget`() {
        val jobId = jobId("timed")

        fakeIot.client().createJob(
            IotJobId.of(jobId.value),
            listOf(thingArn),
            """{"operation":"noop"}""",
            timeoutConfig = org.http4k.connect.amazon.iot.model.TimeoutConfig(inProgressTimeoutInMinutes = 5),
        ).successValue()

        assertThat(
            iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue()
                .execution!!.approximateSecondsBeforeTimedOut,
            equalTo(null)
        )

        val started = iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue().execution!!

        assertThat(started.approximateSecondsBeforeTimedOut, equalTo(300L))
    }

    @Test
    fun `client convenience function targets the fake`() {
        val jobId = jobId("client")

        createJob(jobId, """{"operation":"noop"}""")

        assertThat(
            http.client().describeJobExecution(thingName, JobId.NEXT).successValue().execution!!.jobId,
            equalTo(jobId)
        )
    }
}
