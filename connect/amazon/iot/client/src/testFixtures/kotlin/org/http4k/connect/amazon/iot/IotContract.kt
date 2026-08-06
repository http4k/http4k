package org.http4k.connect.amazon.iot

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.action.JobExecution
import org.http4k.connect.amazon.iot.model.JobExecutionStatus
import org.http4k.connect.amazon.iot.model.JobExecutionStatus.CANCELED
import org.http4k.connect.amazon.iot.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus
import org.http4k.connect.amazon.iot.model.S3Location
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.TargetSelection.SNAPSHOT
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.amazon.iot.model.TimeoutConfig
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.SECONDS

interface IotContract : AwsContract {
    /** For the real service this must be the ARN of an existing thing. */
    val thingArn: ARN

    /** For the real service this must be a role IoT can assume to read the stream's S3 objects. */
    val streamRoleArn: ARN

    /**
     * For the real service this must be an object which actually exists: AWS resolves it while
     * serving CreateStream and answers 404 for a bucket or key it cannot find.
     */
    val streamS3Location: S3Location

    val iot get() = Iot.Http(aws.region, { aws.credentials }, http)

    val thingName get() = ThingName.of(thingArn.value.substringAfterLast(":").substringAfterLast("/"))

    fun jobId(name: String) = JobId.of("http4k-${uuid()}-$name")

    fun streamId(name: String) = StreamId.of("http4k-${uuid()}-$name")

    @Test
    fun `create then describe round-trips the job`() {
        val jobId = jobId("roundtrip")

        try {
            val created = iot.createJob(
                jobId = jobId,
                targets = listOf(thingArn),
                document = SOME_DOCUMENT,
                description = "a roundtrip job",
                timeoutConfig = TimeoutConfig(inProgressTimeoutInMinutes = 5),
            ).successValue()

            assertThat(created.jobId, equalTo(jobId))

            val job = iot.describeJob(jobId).successValue().job

            assertThat(job.jobId, equalTo(jobId))
            assertThat(job.jobArn, equalTo(created.jobArn))
            assertThat(job.targets, equalTo(listOf(thingArn)))
            assertThat(job.targetSelection, equalTo(SNAPSHOT))
            assertThat(job.status, equalTo(JobStatus.IN_PROGRESS))
            assertThat(job.description, equalTo("a roundtrip job"))
            assertThat(job.timeoutConfig, equalTo(TimeoutConfig(inProgressTimeoutInMinutes = 5)))
            assertThat(job.createdAt != null, equalTo(true))
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `create a job with a duplicate id`() {
        val jobId = jobId("duplicate")

        try {
            iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).successValue()

            assertThat(
                iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).failureValue().status,
                equalTo(CONFLICT)
            )
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `describe a job which does not exist`() {
        assertThat(iot.describeJob(jobId("missing")).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `a created job queues one execution for the targeted thing`() {
        val jobId = jobId("execution")

        try {
            iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).successValue()

            val execution = awaitExecution(jobId)

            assertThat(execution.jobId, equalTo(jobId))
            assertThat(execution.status, equalTo(QUEUED))
            assertThat(execution.thingArn, equalTo(thingArn))
            assertThat(execution.versionNumber, equalTo(1L))
            assertThat(execution.executionNumber, equalTo(1L))
            assertThat(execution.queuedAt != null, equalTo(true))
            assertThat(execution.startedAt, equalTo(null))
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `describe a job execution which does not exist`() {
        assertThat(
            iot.describeJobExecution(thingName, jobId("missing")).failureValue().status,
            equalTo(NOT_FOUND)
        )
    }

    @Test
    fun `list the job executions for a thing`() {
        val jobId = jobId("listed")

        try {
            iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).successValue()
            awaitExecution(jobId)

            val listed = iot.listJobExecutionsForThing(thingName, jobId = jobId).successValue()

            assertThat(listed.executionSummaries.map { it.jobId }, equalTo(listOf(jobId)))
            assertThat(listed.executionSummaries[0].jobExecutionSummary.status, equalTo(QUEUED))
            assertThat(listed.executionSummaries[0].jobExecutionSummary.queuedAt != null, equalTo(true))

            val queued = iot.listJobExecutionsForThing(thingName, status = QUEUED, jobId = jobId).successValue()
            assertThat(queued.executionSummaries.map { it.jobId }, equalTo(listOf(jobId)))

            val succeeded = iot
                .listJobExecutionsForThing(thingName, status = JobExecutionStatus.SUCCEEDED, jobId = jobId)
                .successValue()
            assertThat(succeeded.executionSummaries, equalTo(emptyList()))
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `cancel a job cancels its queued executions`() {
        val jobId = jobId("canceled")

        try {
            iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).successValue()
            awaitExecution(jobId)

            assertThat(iot.cancelJob(jobId, comment = "no longer needed").successValue().jobId, equalTo(jobId))

            assertThat(iot.describeJob(jobId).successValue().job.status, equalTo(JobStatus.CANCELED))
            assertThat(iot.describeJob(jobId).successValue().job.comment, equalTo("no longer needed"))
            assertThat(awaitExecutionStatus(jobId, CANCELED).status, equalTo(CANCELED))
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `cancel a job which does not exist`() {
        assertThat(iot.cancelJob(jobId("missing")).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `delete an in-progress job only with force`() {
        val jobId = jobId("deleted")

        try {
            iot.createJob(jobId, listOf(thingArn), SOME_DOCUMENT).successValue()

            assertThat(iot.deleteJob(jobId).failureValue().status, equalTo(CONFLICT))

            iot.deleteJob(jobId, force = true).successValue()

            // deletion may be asynchronous, so the job is either gone or on its way out
            val remains = iot.describeJob(jobId).valueOrNull()
            assertThat(
                remains == null || remains.job.status == JobStatus.DELETION_IN_PROGRESS,
                equalTo(true)
            )
        } finally {
            deleteJobQuietly(jobId)
        }
    }

    @Test
    fun `delete a job which does not exist`() {
        assertThat(iot.deleteJob(jobId("missing")).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `describe the account data endpoint`() {
        val address = iot.describeEndpoint("iot:Data-ATS").successValue().endpointAddress

        assertThat(address.contains(".iot.") && address.endsWith(".amazonaws.com"), equalTo(true))
    }

    /**
     * The dedicated Jobs endpoint type is gone: AWS answers 400 and says to use `iot:Data-ATS`,
     * which is what devices now reach the Jobs data plane on. Pinned as a refusal rather than
     * dropped, because a client which still asks for it gets a puzzling failure otherwise.
     */
    @Test
    fun `the jobs endpoint type is no longer served`() {
        assertThat(iot.describeEndpoint("iot:Jobs").failureValue().status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `create then describe round-trips the stream`() {
        val streamId = streamId("roundtrip")

        try {
            val created = iot.createStream(
                streamId = streamId,
                files = listOf(someFile),
                roleArn = streamRoleArn,
                description = "a roundtrip stream",
            ).successValue()

            assertThat(created.streamId, equalTo(streamId))

            val info = iot.describeStream(streamId).successValue().streamInfo

            assertThat(info.streamId, equalTo(streamId))
            assertThat(info.streamArn, equalTo(created.streamArn))
            assertThat(info.streamVersion, equalTo(created.streamVersion))
            assertThat(info.files, equalTo(listOf(someFile)))
            assertThat(info.roleArn, equalTo(streamRoleArn))
            assertThat(info.description, equalTo("a roundtrip stream"))
            assertThat(info.createdAt != null, equalTo(true))
        } finally {
            deleteStreamQuietly(streamId)
        }
    }

    @Test
    fun `create a stream with a duplicate id`() {
        val streamId = streamId("duplicate")

        try {
            iot.createStream(streamId, listOf(someFile), streamRoleArn).successValue()

            assertThat(
                iot.createStream(streamId, listOf(someFile), streamRoleArn).failureValue().status,
                equalTo(CONFLICT)
            )
        } finally {
            deleteStreamQuietly(streamId)
        }
    }

    @Test
    fun `create a stream with no files`() {
        assertThat(
            iot.createStream(streamId("nofiles"), emptyList(), streamRoleArn).failureValue().status,
            equalTo(BAD_REQUEST)
        )
    }

    /**
     * The absolute version a new stream starts at is AWS's to choose, so only the increment is
     * asserted here; the fake's own starting value is pinned in its test.
     */
    @Test
    fun `updating a stream bumps its version`() {
        val streamId = streamId("update")
        val replacement = StreamFile(fileId = 1, s3Location = someFile.s3Location)

        try {
            val created = iot.createStream(streamId, listOf(someFile), streamRoleArn).successValue()

            val updated = iot.updateStream(
                streamId = streamId,
                files = listOf(replacement),
                description = "now with a different file",
            ).successValue()

            assertThat(updated.streamVersion > created.streamVersion, equalTo(true))

            val info = iot.describeStream(streamId).successValue().streamInfo

            assertThat(info.files, equalTo(listOf(replacement)))
            assertThat(info.description, equalTo("now with a different file"))
        } finally {
            deleteStreamQuietly(streamId)
        }
    }

    @Test
    fun `describe a stream which does not exist`() {
        assertThat(iot.describeStream(streamId("missing")).failureValue().status, equalTo(NOT_FOUND))
    }

    @Test
    fun `delete a stream which does not exist`() {
        assertThat(iot.deleteStream(streamId("missing")).failureValue().status, equalTo(NOT_FOUND))
    }
}

private val SOME_DOCUMENT = """{"operation":"noop"}"""

private val IotContract.someFile get() = StreamFile(fileId = 0, s3Location = streamS3Location)

/**
 * The result is ignored on purpose. This runs in a finally block, so a failure here would
 * hide the assertion failure that got us there.
 */
private fun IotContract.deleteStreamQuietly(streamId: StreamId) {
    iot.deleteStream(streamId)
}

/**
 * AWS materialises the executions of a new job asynchronously, so reads which follow a
 * CreateJob are retried until the execution appears.
 */
private fun IotContract.awaitExecution(jobId: JobId): JobExecution {
    val deadline = System.currentTimeMillis() + SECONDS.toMillis(20)

    while (System.currentTimeMillis() < deadline) {
        iot.describeJobExecution(thingName, jobId).valueOrNull()?.let { return it.execution }
        Thread.sleep(500)
    }

    return iot.describeJobExecution(thingName, jobId).successValue().execution
}

/** Cancellation propagates to executions asynchronously, so the expected status is awaited. */
private fun IotContract.awaitExecutionStatus(jobId: JobId, expected: JobExecutionStatus): JobExecution {
    val deadline = System.currentTimeMillis() + SECONDS.toMillis(20)

    while (System.currentTimeMillis() < deadline) {
        val execution = iot.describeJobExecution(thingName, jobId).valueOrNull()?.execution
        if (execution?.status == expected) return execution
        Thread.sleep(500)
    }

    return iot.describeJobExecution(thingName, jobId).successValue().execution
}

/**
 * The result is ignored on purpose. This runs in a finally block, so a failure here would
 * hide the assertion failure that got us there.
 */
private fun IotContract.deleteJobQuietly(jobId: JobId) {
    iot.deleteJob(jobId, force = true)
}
