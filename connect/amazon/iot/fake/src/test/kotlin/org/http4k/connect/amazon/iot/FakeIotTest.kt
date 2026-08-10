package org.http4k.connect.amazon.iot

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus
import org.http4k.connect.amazon.iot.model.S3Location
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.TargetSelection.SNAPSHOT
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.failureValue
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC

class FakeIotTest : IotContract, FakeAwsContract {
    override val http = FakeIot()

    override val thingArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/my-thing")

    override val streamRoleArn = ARN.of("arn:aws:iam::000000000000:role/http4k-stream")

    override val streamS3Location = S3Location(bucket = "http4k-bucket", key = "image.bin")

    @Test
    fun `stores the created job with one queued execution per target`() {
        val jobId = jobId("stored")
        val otherThing = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/other-thing")

        iot.createJob(jobId, listOf(thingArn, otherThing), """{"operation":"noop"}""").successValue()

        val stored = http.job(jobId)!!

        assertThat(stored.status, equalTo(JobStatus.IN_PROGRESS))
        assertThat(stored.targetSelection, equalTo(SNAPSHOT))
        assertThat(stored.document, equalTo("""{"operation":"noop"}"""))
        assertThat(
            stored.executions.keys,
            equalTo(setOf(ThingName.of("my-thing"), ThingName.of("other-thing")))
        )
        assertThat(stored.executions.values.map { it.status }.distinct(), equalTo(listOf(QUEUED)))
        assertThat(stored.executions.values.map { it.thingArn }, equalTo(listOf(thingArn, otherThing)))
    }

    /** CreateJob requires a document, so this can only be asked over the wire. */
    @Test
    fun `a job without a document is refused`() {
        val response = http(
            Request(PUT, "/jobs/undocumented").body("""{"targets":["${thingArn.value}"]}""")
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a job without targets is refused`() {
        assertThat(
            iot.createJob(jobId("untargeted"), emptyList(), """{}""").failureValue().status,
            equalTo(BAD_REQUEST)
        )
    }

    @Test
    fun `jobs are seeded from the storage the fake is given and timed by its clock`() {
        val store = Storage.InMemory<StoredJob>()
        val clock = Clock.fixed(Instant.parse("2021-02-26T15:26:33Z"), UTC)
        val fake = FakeIot(store, clock = clock)
        val jobId = JobId.of("seeded")

        fake.client().createJob(jobId, listOf(thingArn), """{}""").successValue()

        assertThat(store[jobId.value]!!.createdAt, equalTo(Timestamp.of(clock.instant())))

        val described = FakeIot(store).client().describeJob(jobId).successValue().job
        assertThat(described.jobId, equalTo(jobId))
        assertThat(described.createdAt, equalTo(Timestamp.of(clock.instant())))
    }

    @Test
    fun `delete removes the job from the store`() {
        val jobId = jobId("removed")

        iot.createJob(jobId, listOf(thingArn), """{}""").successValue()
        iot.deleteJob(jobId, force = true).successValue()

        assertThat(http.job(jobId), equalTo(null))
    }

    @Test
    fun `cancelling with force bumps the version of the canceled executions`() {
        val jobId = jobId("versioned")

        iot.createJob(jobId, listOf(thingArn), """{}""").successValue()
        iot.cancelJob(jobId, force = true).successValue()

        assertThat(http.job(jobId)!!.executions.values.single().versionNumber, equalTo(2L))
        assertThat(http.job(jobId)!!.forceCanceled, equalTo(true))
    }

    @Test
    fun `the endpoint address is synthesized per endpoint type`() {
        val region = Region.of("ldn-north-1")

        assertThat(
            iot.describeEndpoint().successValue().endpointAddress,
            equalTo("http4k000000-ats.iot.$region.amazonaws.com")
        )
        assertThat(
            iot.describeEndpoint("iot:Data-ATS").successValue().endpointAddress,
            equalTo("http4k000000-ats.iot.$region.amazonaws.com")
        )
        assertThat(
            iot.describeEndpoint("iot:Data").successValue().endpointAddress,
            equalTo("http4k000000.iot.$region.amazonaws.com")
        )
        assertThat(
            iot.describeEndpoint("iot:CredentialProvider").successValue().endpointAddress,
            equalTo("http4k000000.credentials.iot.$region.amazonaws.com")
        )
        assertThat(iot.describeEndpoint("iot:Nonsense").failureValue().status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `client convenience function targets the fake`() {
        val jobId = jobId("client")

        http.client().createJob(jobId, listOf(thingArn), """{}""").successValue()

        assertThat(http.job(jobId)!!.jobId, equalTo(jobId))
    }

    /** The contract asserts only that an update increments; this pins where the fake starts. */
    @Test
    fun `a new stream is at version zero and each update adds one`() {
        val streamId = streamId("versions")

        assertThat(
            iot.createStream(streamId, listOf(someFile), streamRoleArn).successValue().streamVersion,
            equalTo(0)
        )
        assertThat(iot.updateStream(streamId, description = "one").successValue().streamVersion, equalTo(1))
        assertThat(iot.updateStream(streamId, description = "two").successValue().streamVersion, equalTo(2))
    }

    @Test
    fun `an update leaves the members it does not name`() {
        val streamId = streamId("partial")

        iot.createStream(streamId, listOf(someFile), streamRoleArn, description = "original").successValue()
        iot.updateStream(streamId, description = "renamed").successValue()

        val info = iot.describeStream(streamId).successValue().streamInfo

        assertThat(info.files, equalTo(listOf(someFile)))
        assertThat(info.roleArn, equalTo(streamRoleArn))
        assertThat(info.description, equalTo("renamed"))
    }

    @Test
    fun `a stream without a role is refused`() {
        val response = http(
            Request(POST, "/streams/no-role").body("""{"files":[{"fileId":0,"s3Location":{"bucket":"b","key":"k"}}]}""")
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a file id outside 0 to 255 is refused`() {
        val outOfRange = StreamFile(fileId = 256, s3Location = someFile.s3Location)

        assertThat(
            iot.createStream(streamId("badfile"), listOf(outOfRange), streamRoleArn).failureValue().status,
            equalTo(BAD_REQUEST)
        )
    }

    /**
     * Listing is not in the contract: it is account-wide, so against a real account the
     * assertion would depend on what else is there.
     */
    @Test
    fun `lists streams newest first, and oldest first on request`() {
        val clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), UTC)
        val fake = FakeIot(clock = clock)
        val client = fake.client()
        val first = StreamId.of("first")
        val second = StreamId.of("second")

        client.createStream(first, listOf(someFile), streamRoleArn).successValue()
        client.createStream(second, listOf(someFile), streamRoleArn).successValue()

        // The clock is fixed, so createdAt ties and the streamId breaks it - which is what
        // makes the order assertable at all.
        assertThat(
            client.listStreams().successValue().streams.map { it.streamId },
            equalTo(listOf(second, first))
        )
        assertThat(
            client.listStreams(ascendingOrder = true).successValue().streams.map { it.streamId },
            equalTo(listOf(first, second))
        )
    }

    @Test
    fun `a stream page carries a token for the rest`() {
        val fake = FakeIot()
        val client = fake.client()
        val ids = (1..3).map { StreamId.of("stream-$it") }
        ids.forEach { client.createStream(it, listOf(someFile), streamRoleArn).successValue() }

        val page = client.listStreams(maxResults = 2, ascendingOrder = true).successValue()

        assertThat(page.streams.size, equalTo(2))

        val rest = client.listStreams(nextToken = page.nextToken, ascendingOrder = true).successValue()

        assertThat(rest.streams.size, equalTo(1))
        assertThat(rest.nextToken, equalTo(null))
    }

    @Test
    fun `stores the created stream`() {
        val streamId = streamId("stored")

        iot.createStream(streamId, listOf(someFile), streamRoleArn, description = "stored").successValue()

        val stored = http.stream(streamId)!!

        assertThat(stored.files, equalTo(listOf(someFile)))
        assertThat(stored.roleArn, equalTo(streamRoleArn))
        assertThat(stored.description, equalTo("stored"))
    }

    @Test
    fun `a thing group target creates no execution`() {
        val jobId = jobId("group")
        val group = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thinggroup/my-group")

        iot.createJob(jobId, listOf(thingArn, group), """{"operation":"noop"}""").successValue()

        assertThat(http.job(jobId)!!.executions.keys, equalTo(setOf(ThingName.of("my-thing"))))
    }

    @Test
    fun `an update is held to the same file constraints as a create`() {
        val streamId = streamId("constraints")
        val tooMany = (0..50).map { StreamFile(fileId = 0, s3Location = someFile.s3Location) }
        val outOfRange = StreamFile(fileId = 256, s3Location = someFile.s3Location)

        iot.createStream(streamId, listOf(someFile), streamRoleArn).successValue()

        assertThat(iot.updateStream(streamId, files = tooMany).failureValue().status, equalTo(BAD_REQUEST))
        assertThat(iot.updateStream(streamId, files = listOf(outOfRange)).failureValue().status, equalTo(BAD_REQUEST))
        assertThat(iot.updateStream(streamId, files = emptyList()).failureValue().status, equalTo(BAD_REQUEST))
    }

    /** AWS answers 400 for a malformed query value; throwing out of the handler would be a 500. */
    @Test
    fun `malformed query values are refused rather than thrown`() {
        val jobId = jobId("query")
        iot.createJob(jobId, listOf(thingArn), """{"operation":"noop"}""").successValue()

        val refused = listOf(
            Request(GET, "/things/my-thing/jobs").query("status", "NONSENSE"),
            Request(GET, "/things/my-thing/jobs").query("jobId", "not a job id"),
            Request(GET, "/things/my-thing/jobs").query("maxResults", "many"),
            Request(GET, "/things/my-thing/jobs").query("nextToken", "somewhere"),
            Request(GET, "/things/my-thing/jobs/${jobId.value}").query("executionNumber", "first"),
            Request(PUT, "/jobs/${jobId.value}/cancel").query("force", "yes"),
            Request(GET, "/streams").query("isAscendingOrder", "yes"),
        )

        refused.forEach { assertThat(it.uri.toString(), http(it).status, equalTo(BAD_REQUEST)) }
    }

    /** The refused value is echoed into the message, so it must not be able to break out of the JSON. */
    @Test
    fun `a refused value containing a quote still yields readable json`() {
        val response = http(Request(GET, "/things/my-thing/jobs").query("status", """not" a status"""))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            IotMoshi.asA<Map<String, String>>(response.bodyString())["message"],
            equalTo("""not" a status is not a job execution status""")
        )
    }

    /** Likewise for an identifier in the path: outside the AWS charset is a 400, not a 500. */
    @Test
    fun `malformed path identifiers are refused rather than thrown`() {
        val refused = listOf(
            Request(PUT, "/jobs/not.a.job.id").body("""{"targets":["${thingArn.value}"],"document":"{}"}"""),
            Request(GET, "/jobs/not.a.job.id"),
            Request(DELETE, "/jobs/not.a.job.id"),
            Request(PUT, "/jobs/not.a.job.id/cancel"),
            Request(GET, "/things/not.a.thing/jobs"),
            Request(GET, "/things/not.a.thing/jobs/my-job"),
            Request(POST, "/streams/not.a.stream.id"),
            Request(GET, "/streams/not.a.stream.id"),
            Request(PUT, "/streams/not.a.stream.id"),
            Request(DELETE, "/streams/not.a.stream.id"),
        )

        refused.forEach { assertThat(it.uri.toString(), http(it).status, equalTo(BAD_REQUEST)) }
    }

    private val someFile = StreamFile(fileId = 0, s3Location = S3Location(bucket = "bucket", key = "image.bin"))
}
