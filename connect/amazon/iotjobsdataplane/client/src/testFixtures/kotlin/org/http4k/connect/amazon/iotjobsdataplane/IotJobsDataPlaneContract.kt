package org.http4k.connect.amazon.iotjobsdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.IN_PROGRESS
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.SUCCEEDED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.SECONDS

/**
 * The jobs data plane cannot create jobs, so implementations provide [createJob] - through
 * FakeIot over a shared store, or through the real control plane. The selection cases assume
 * [thingName] has no other pending job executions.
 */
interface IotJobsDataPlaneContract : AwsContract {
    val thingName: ThingName

    /** Seeds a job carrying [document], targeted at [thingName]. */
    fun createJob(jobId: JobId, document: String)

    /** Force-deletes a seeded job. Failures are ignored: this runs in finally blocks. */
    fun cleanupJob(jobId: JobId)

    val iotJobsDataPlane get() = IotJobsDataPlane.Http(aws.region, { aws.credentials }, http)

    fun jobId(name: String) = JobId.of("http4k-${uuid()}-$name")

    @Test
    fun `an idle thing has nothing pending`() {
        val pending = iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()

        assertThat(pending.inProgressJobs, equalTo(emptyList()))
        assertThat(pending.queuedJobs, equalTo(emptyList()))
    }

    @Test
    fun `a created job appears queued for the thing`() {
        val jobId = jobId("queued")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            val summary = iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()
                .queuedJobs.single { it.jobId == jobId }

            assertThat(summary.versionNumber, equalTo(1L))
            assertThat(summary.executionNumber, equalTo(1L))
            assertThat(summary.queuedAt != null, equalTo(true))
            assertThat(summary.startedAt, equalTo(null))
        } finally {
            cleanupJob(jobId)
        }
    }

    /**
     * The firmware polls `$next` on every connect precisely because it is side-effect free,
     * so the read-only property is pinned here: describing must not transition anything.
     */
    @Test
    fun `describing the next pending execution is read-only`() {
        val jobId = jobId("readonly")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            val first = iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT, includeJobDocument = true)
                .successValue().execution!!

            assertThat(first.jobId, equalTo(jobId))
            assertThat(first.status, equalTo(QUEUED))
            assertThat(first.versionNumber, equalTo(1L))
            assertThat(first.jobDocument, equalTo(SOME_DOCUMENT))

            val second = iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT)
                .successValue().execution!!

            assertThat(second.status, equalTo(QUEUED))
            assertThat(second.versionNumber, equalTo(1L))
            // the AWS API reference: the document is included unless the query says false
            assertThat(second.jobDocument, equalTo(SOME_DOCUMENT))

            val pending = iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()
            assertThat(pending.queuedJobs.map { it.jobId }, equalTo(listOf(jobId)))
            assertThat(pending.inProgressJobs, equalTo(emptyList()))
        } finally {
            cleanupJob(jobId)
        }
    }

    @Test
    fun `the job document is excluded on request`() {
        val jobId = jobId("undocumented")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            val execution = iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT, includeJobDocument = false)
                .successValue().execution!!

            assertThat(execution.jobDocument, equalTo(null))
        } finally {
            cleanupJob(jobId)
        }
    }

    /** With nothing pending, `$next` answers 200 with an empty document rather than an error. */
    @Test
    fun `describing next with nothing pending`() {
        assertThat(
            iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution,
            equalTo(null)
        )
    }

    @Test
    fun `starting the next pending execution claims it`() {
        val jobId = jobId("started")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            val execution = iotJobsDataPlane
                .startNextPendingJobExecution(thingName, statusDetails = mapOf("step" to "downloading"))
                .successValue().execution!!

            assertThat(execution.jobId, equalTo(jobId))
            assertThat(execution.status, equalTo(IN_PROGRESS))
            assertThat(execution.versionNumber, equalTo(2L))
            assertThat(execution.startedAt != null, equalTo(true))
            assertThat(execution.statusDetails, equalTo(mapOf("step" to "downloading")))
            assertThat(execution.jobDocument, equalTo(SOME_DOCUMENT))

            val pending = iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()
            assertThat(pending.inProgressJobs.map { it.jobId }, equalTo(listOf(jobId)))
            assertThat(pending.queuedJobs, equalTo(emptyList()))
        } finally {
            cleanupJob(jobId)
        }
    }

    /** AWS does not touch an already-IN_PROGRESS pick: no new details, no version bump. */
    @Test
    fun `starting again returns the in-progress execution untouched`() {
        val jobId = jobId("restarted")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            iotJobsDataPlane.startNextPendingJobExecution(thingName, statusDetails = mapOf("step" to "downloading"))
                .successValue()

            val again = iotJobsDataPlane
                .startNextPendingJobExecution(thingName, statusDetails = mapOf("step" to "ignored"))
                .successValue().execution!!

            assertThat(again.jobId, equalTo(jobId))
            assertThat(again.status, equalTo(IN_PROGRESS))
            assertThat(again.versionNumber, equalTo(2L))
            assertThat(again.statusDetails, equalTo(mapOf("step" to "downloading")))
        } finally {
            cleanupJob(jobId)
        }
    }

    @Test
    fun `starting with nothing pending`() {
        assertThat(
            iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue().execution,
            equalTo(null)
        )
    }

    @Test
    fun `a device walks an execution to success`() {
        val jobId = jobId("walked")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()

            iotJobsDataPlane.updateJobExecution(
                thingName, jobId, IN_PROGRESS,
                statusDetails = mapOf("step" to "flashing"),
                expectedVersion = 2,
            ).successValue()

            val updated = iotJobsDataPlane.updateJobExecution(
                thingName, jobId, SUCCEEDED,
                statusDetails = mapOf("firmware" to "confirmed"),
                expectedVersion = 3,
                includeJobExecutionState = true,
            ).successValue()

            val executionState = updated.executionState!!
            assertThat(executionState.status, equalTo(SUCCEEDED))
            assertThat(executionState.versionNumber, equalTo(4L))
            assertThat(executionState.statusDetails, equalTo(mapOf("firmware" to "confirmed")))

            assertThat(
                iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution,
                equalTo(null)
            )
        } finally {
            cleanupJob(jobId)
        }
    }

    @Test
    fun `an update with a mismatched expected version is rejected`() {
        val jobId = jobId("stale")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()

            assertThat(
                iotJobsDataPlane.updateJobExecution(thingName, jobId, SUCCEEDED, expectedVersion = 1)
                    .failureValue().status,
                equalTo(CONFLICT)
            )
        } finally {
            cleanupJob(jobId)
        }
    }

    @Test
    fun `an update to a terminal execution is rejected`() {
        val jobId = jobId("terminal")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()
            iotJobsDataPlane.updateJobExecution(thingName, jobId, SUCCEEDED).successValue()

            assertThat(
                iotJobsDataPlane.updateJobExecution(thingName, jobId, IN_PROGRESS).failureValue().status,
                equalTo(CONFLICT)
            )
        } finally {
            cleanupJob(jobId)
        }
    }

    /**
     * The docs declare TerminalStateException (410) for DescribeJobExecution, which implies
     * that a terminal execution addressed by its explicit jobId is refused rather than shown.
     */
    @Test
    fun `describing a terminal execution by its id is refused`() {
        val jobId = jobId("done")

        try {
            createJob(jobId, SOME_DOCUMENT)
            awaitQueued(jobId)

            iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()
            iotJobsDataPlane.updateJobExecution(thingName, jobId, SUCCEEDED).successValue()

            assertThat(
                iotJobsDataPlane.describeJobExecution(thingName, jobId).failureValue().status,
                equalTo(GONE)
            )
        } finally {
            cleanupJob(jobId)
        }
    }

    @Test
    fun `describe an execution which does not exist`() {
        assertThat(
            iotJobsDataPlane.describeJobExecution(thingName, jobId("missing")).failureValue().status,
            equalTo(NOT_FOUND)
        )
    }

    @Test
    fun `update an execution which does not exist`() {
        assertThat(
            iotJobsDataPlane.updateJobExecution(thingName, jobId("missing"), IN_PROGRESS).failureValue().status,
            equalTo(NOT_FOUND)
        )
    }
}

private val SOME_DOCUMENT = """{"operation":"firmware-update","version":"1.2.3"}"""

/**
 * AWS materialises the executions of a new job asynchronously, so seeded tests wait for the
 * execution to appear in the pending listing before asserting on it.
 */
private fun IotJobsDataPlaneContract.awaitQueued(jobId: JobId) {
    val deadline = System.currentTimeMillis() + SECONDS.toMillis(20)

    while (System.currentTimeMillis() < deadline) {
        val pending = iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()
        if (pending.queuedJobs.any { it.jobId == jobId }) return
        Thread.sleep(500)
    }

    assertThat(
        iotJobsDataPlane.getPendingJobExecutions(thingName).successValue().queuedJobs.map { it.jobId },
        equalTo(listOf(jobId))
    )
}
