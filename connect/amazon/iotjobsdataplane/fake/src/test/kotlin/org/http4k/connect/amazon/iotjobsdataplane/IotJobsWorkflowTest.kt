package org.http4k.connect.amazon.iotjobsdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.FakeIot
import org.http4k.connect.amazon.iot.StoredJob
import org.http4k.connect.amazon.iot.cancelJob
import org.http4k.connect.amazon.iot.createJob
import org.http4k.connect.amazon.iot.describeJob
import org.http4k.connect.amazon.iot.describeJobExecution
import org.http4k.connect.amazon.iot.model.TargetSelection.CONTINUOUS
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.IN_PROGRESS
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.SUCCEEDED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.failureValue
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.CONFLICT
import org.junit.jupiter.api.Test
import org.http4k.connect.amazon.iot.model.JobExecutionStatus as IotJobExecutionStatus
import org.http4k.connect.amazon.iot.model.JobId as IotJobId
import org.http4k.connect.amazon.iot.model.JobStatus as IotJobStatus
import org.http4k.connect.amazon.iot.model.ThingName as IotThingName

/**
 * One job store backs both FakeIot and FakeIotJobsDataPlane, so a Jobs workflow runs
 * end-to-end without AWS: the control plane creates a job with a document, the device walks
 * it through the data plane, and the control plane observes the terminal result.
 */
class IotJobsWorkflowTest {

    private val store = Storage.InMemory<StoredJob>()
    private val controlPlane = FakeIot(store).client()
    private val devicePlane = FakeIotJobsDataPlane(store).client()

    private val thingName = ThingName.of("my-thing")
    private val thingArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/${thingName.value}")
    private val jobId = JobId.of("ota-1-2-3")
    private val document = """{"operation":"firmware-update","url":"https://firmware/firmware.bin","version":"1.2.3"}"""

    private fun iotJobId(jobId: JobId) = IotJobId.of(jobId.value)

    private fun createJob(jobId: JobId, targets: List<ARN> = listOf(thingArn)) {
        controlPlane.createJob(iotJobId(jobId), targets, document).successValue()
    }

    private fun controlPlaneExecution(thingName: ThingName, jobId: JobId) = controlPlane
        .describeJobExecution(IotThingName.of(thingName.value), iotJobId(jobId))
        .successValue()
        .execution

    @Test
    fun `a job created by the cloud is walked to success by the device and completes`() {
        createJob(jobId)

        // the device sees it queued
        val pending = devicePlane.getPendingJobExecutions(thingName).successValue()
        assertThat(pending.queuedJobs.map { it.jobId }, equalTo(listOf(jobId)))
        assertThat(pending.inProgressJobs, equalTo(emptyList()))

        // describing $next returns the document without transitioning anything
        val described = devicePlane.describeJobExecution(thingName, JobId.NEXT, includeJobDocument = true)
            .successValue().execution!!
        assertThat(described.jobId, equalTo(jobId))
        assertThat(described.status, equalTo(QUEUED))
        assertThat(described.jobDocument, equalTo(document))

        // still queued: the describe was read-only
        val again = devicePlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution!!
        assertThat(again.status, equalTo(QUEUED))
        assertThat(again.versionNumber, equalTo(1L))

        // the device claims it
        val started = devicePlane.startNextPendingJobExecution(thingName, statusDetails = mapOf("step" to "downloading"))
            .successValue().execution!!
        assertThat(started.status, equalTo(IN_PROGRESS))
        assertThat(started.versionNumber, equalTo(2L))
        assertThat(started.startedAt != null, equalTo(true))
        assertThat(started.jobDocument, equalTo(document))

        // progress, then terminal success
        devicePlane.updateJobExecution(
            thingName, jobId, IN_PROGRESS,
            statusDetails = mapOf("step" to "flashing"),
            expectedVersion = 2,
        ).successValue()

        val updated = devicePlane.updateJobExecution(
            thingName, jobId, SUCCEEDED,
            statusDetails = mapOf("firmware" to "confirmed"),
            expectedVersion = 3,
            includeJobExecutionState = true,
        ).successValue()
        assertThat(updated.executionState!!.status, equalTo(SUCCEEDED))
        assertThat(updated.executionState!!.versionNumber, equalTo(4L))

        // the cloud observes the terminal execution...
        val execution = controlPlaneExecution(thingName, jobId)
        assertThat(execution.status, equalTo(IotJobExecutionStatus.SUCCEEDED))
        assertThat(execution.statusDetails!!.detailsMap, equalTo(mapOf("firmware" to "confirmed")))

        // ...and the completed job
        val job = controlPlane.describeJob(iotJobId(jobId)).successValue().job
        assertThat(job.status, equalTo(IotJobStatus.COMPLETED))
        assertThat(job.completedAt != null, equalTo(true))

        // nothing is pending for the device any more
        val done = devicePlane.getPendingJobExecutions(thingName).successValue()
        assertThat(done.queuedJobs, equalTo(emptyList()))
        assertThat(done.inProgressJobs, equalTo(emptyList()))
    }

    @Test
    fun `an update with a stale expected version is rejected`() {
        createJob(jobId)
        devicePlane.startNextPendingJobExecution(thingName).successValue()

        assertThat(
            devicePlane.updateJobExecution(thingName, jobId, SUCCEEDED, expectedVersion = 1)
                .failureValue().status,
            equalTo(CONFLICT)
        )
    }

    @Test
    fun `a terminal execution refuses further updates`() {
        createJob(jobId)
        devicePlane.startNextPendingJobExecution(thingName).successValue()
        devicePlane.updateJobExecution(thingName, jobId, SUCCEEDED).successValue()

        assertThat(
            devicePlane.updateJobExecution(thingName, jobId, IN_PROGRESS).failureValue().status,
            equalTo(CONFLICT)
        )
    }

    /**
     * A continuous job's target set is open, so AWS keeps it in progress to enrol things added
     * later - a caller waiting for COMPLETED on one would wait forever.
     */
    @Test
    fun `a continuous job does not complete when every execution is terminal`() {
        val continuous = JobId.of("continuous-1")
        controlPlane.createJob(
            iotJobId(continuous), listOf(thingArn), document, targetSelection = CONTINUOUS,
        ).successValue()

        devicePlane.startNextPendingJobExecution(thingName).successValue()
        devicePlane.updateJobExecution(thingName, continuous, SUCCEEDED).successValue()

        assertThat(controlPlaneExecution(thingName, continuous).status, equalTo(IotJobExecutionStatus.SUCCEEDED))
        assertThat(
            controlPlane.describeJob(iotJobId(continuous)).successValue().job.status,
            equalTo(IotJobStatus.IN_PROGRESS)
        )
    }

    @Test
    fun `a force cancel marks the execution it took down`() {
        createJob(jobId)

        devicePlane.startNextPendingJobExecution(thingName).successValue()

        controlPlane.cancelJob(iotJobId(jobId), force = true).successValue()

        val execution = controlPlaneExecution(thingName, jobId)

        assertThat(execution.status, equalTo(IotJobExecutionStatus.CANCELED))
        assertThat(execution.forceCanceled, equalTo(true))
    }

    /** A queued execution would have been canceled anyway, so force is not what took it down. */
    @Test
    fun `a queued execution canceled by force is not marked`() {
        createJob(jobId)

        controlPlane.cancelJob(iotJobId(jobId), force = true).successValue()

        assertThat(controlPlaneExecution(thingName, jobId).forceCanceled, equalTo(null))
    }

    @Test
    fun `cancelling without force spares an in-progress execution`() {
        val otherThing = ThingName.of("other-thing")
        val otherArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/${otherThing.value}")

        createJob(jobId, targets = listOf(thingArn, otherArn))

        // the first thing has started; the second is still queued
        devicePlane.startNextPendingJobExecution(thingName).successValue()

        controlPlane.cancelJob(iotJobId(jobId), comment = "rollback").successValue()

        val job = controlPlane.describeJob(iotJobId(jobId)).successValue().job
        assertThat(job.status, equalTo(IotJobStatus.CANCELED))
        assertThat(job.comment, equalTo("rollback"))

        assertThat(controlPlaneExecution(thingName, jobId).status, equalTo(IotJobExecutionStatus.IN_PROGRESS))
        assertThat(controlPlaneExecution(otherThing, jobId).status, equalTo(IotJobExecutionStatus.CANCELED))
    }

    @Test
    fun `cancelling with force cancels the in-progress execution too`() {
        createJob(jobId)
        devicePlane.startNextPendingJobExecution(thingName).successValue()

        controlPlane.cancelJob(iotJobId(jobId), force = true).successValue()

        assertThat(controlPlaneExecution(thingName, jobId).status, equalTo(IotJobExecutionStatus.CANCELED))
    }

    /** A canceled job no longer offers work to the device. */
    @Test
    fun `a canceled job is not served as next`() {
        createJob(jobId)

        controlPlane.cancelJob(iotJobId(jobId)).successValue()

        assertThat(devicePlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution, equalTo(null))
        assertThat(devicePlane.startNextPendingJobExecution(thingName).successValue().execution, equalTo(null))
    }
}
