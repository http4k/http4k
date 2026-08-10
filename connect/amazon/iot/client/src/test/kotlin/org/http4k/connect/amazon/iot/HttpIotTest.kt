package org.http4k.connect.amazon.iot

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.action.DescribeJobExecution
import org.http4k.connect.amazon.iot.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus.IN_PROGRESS
import org.http4k.connect.amazon.iot.model.S3Location
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.TargetSelection.SNAPSHOT
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.amazon.iot.model.TimeoutConfig
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.MockHttp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class HttpIotTest {

    private val thingArn = ARN.of("arn:aws:iot:us-east-1:000000000000:thing/my-thing")
    private val jobId = JobId.of("my-job")
    private val thingName = ThingName.of("my-thing")
    private val streamId = StreamId.of("my-stream")
    private val roleArn = ARN.of("arn:aws:iam::000000000000:role/my-stream-role")

    private val mockHttp = MockHttp(Response(OK).body(CREATED_JOB))

    private val iot = clientFor(mockHttp)

    private fun clientFor(http: MockHttp) =
        Iot.Http(Region.US_EAST_1, CredentialsProvider.FakeAwsEnvironment(), http)

    private fun uri(path: String) = Uri.of("https://iot.us-east-1.amazonaws.com$path")

    @Test
    fun `create job builds the documented request`() {
        iot.createJob(
            jobId = jobId,
            targets = listOf(thingArn),
            document = """{"operation":"noop"}""",
            description = "a job",
            targetSelection = SNAPSHOT,
            timeoutConfig = TimeoutConfig(inProgressTimeoutInMinutes = 5),
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(PUT))
        assertThat(request, hasUri(uri("/jobs/my-job")))
        assertThat(request, hasHeader("Content-Type", "application/json; charset=utf-8"))
        assertThat(
            request, hasBody(
                """{"targets":["${thingArn.value}"],"document":"{\"operation\":\"noop\"}",""" +
                    """"description":"a job","targetSelection":"SNAPSHOT",""" +
                    """"timeoutConfig":{"inProgressTimeoutInMinutes":5}}"""
            )
        )
    }

    @Test
    fun `create job omits every optional field when not set`() {
        iot.createJob(jobId, listOf(thingArn), """{}""").successValue()

        assertThat(mockHttp.request!!, hasBody("""{"targets":["${thingArn.value}"],"document":"{}"}"""))
    }

    @Test
    fun `create job unmarshals the response document`() {
        val created = iot.createJob(jobId, listOf(thingArn), """{}""").successValue()

        assertThat(created.jobId, equalTo(jobId))
        assertThat(created.jobArn, equalTo(ARN.of("arn:aws:iot:us-east-1:000000000000:job/my-job")))
        assertThat(created.description, equalTo("a job"))
    }

    /** Every current AWS SDK signs the IoT control plane as service `iot` (older ones used `execute-api`). */
    @Test
    fun `requests are signed with the iot service name against the iot endpoint`() {
        iot.createJob(jobId, listOf(thingArn), """{}""").successValue()

        val request = mockHttp.request!!

        assertThat(request.uri.host, equalTo("iot.us-east-1.amazonaws.com"))
        assertThat(request.header("Authorization")!!, containsSubstring("/us-east-1/iot/aws4_request"))
    }

    @Test
    fun `the endpoint can be overridden without changing the signing name`() {
        val mock = MockHttp(Response(OK).body(CREATED_JOB))

        Iot.Http(
            Region.US_EAST_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mock,
            overrideEndpoint = Uri.of("http://localhost:12345")
        ).createJob(jobId, listOf(thingArn), """{}""").successValue()

        assertThat(mock.request!!.uri.host, equalTo("localhost"))
        assertThat(mock.request!!.header("Authorization")!!, containsSubstring("/us-east-1/iot/aws4_request"))
    }

    @Test
    fun `describe job builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body(DESCRIBED_JOB))
        val job = clientFor(mock).describeJob(jobId).successValue().job

        assertThat(mock.request!!, hasMethod(GET))
        assertThat(mock.request!!, hasUri(uri("/jobs/my-job")))

        assertThat(job.jobId, equalTo(jobId))
        assertThat(job.targets, equalTo(listOf(thingArn)))
        assertThat(job.targetSelection, equalTo(SNAPSHOT))
        assertThat(job.status, equalTo(IN_PROGRESS))
        assertThat(job.createdAt, equalTo(Timestamp.of(1614355593)))
        assertThat(job.timeoutConfig, equalTo(TimeoutConfig(inProgressTimeoutInMinutes = 5)))
    }

    @Test
    fun `describe job execution builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body(DESCRIBED_EXECUTION))
        val execution = clientFor(mock).describeJobExecution(thingName, jobId, executionNumber = 2)
            .successValue()
            .execution

        assertThat(mock.request!!, hasMethod(GET))
        assertThat(mock.request!!, hasUri(uri("/things/my-thing/jobs/my-job?executionNumber=2")))

        assertThat(execution.jobId, equalTo(jobId))
        assertThat(execution.status, equalTo(QUEUED))
        assertThat(execution.thingArn, equalTo(thingArn))
        assertThat(execution.statusDetails!!.detailsMap, equalTo(mapOf("step" to "queued")))
        assertThat(execution.versionNumber, equalTo(1L))
        assertThat(execution.queuedAt, equalTo(Timestamp.of(1614355593)))
    }

    @Test
    fun `describe job execution omits the execution number when not set`() {
        val mock = MockHttp(Response(OK).body(DESCRIBED_EXECUTION))

        clientFor(mock).describeJobExecution(thingName, jobId).successValue()

        assertThat(mock.request!!, hasUri(uri("/things/my-thing/jobs/my-job")))
    }

    @Test
    fun `list job executions for thing builds the documented request and unmarshals the response`() {
        val mock = MockHttp(
            Response(OK).body(
                """{"executionSummaries":[{"jobId":"my-job","jobExecutionSummary":""" +
                    """{"status":"QUEUED","queuedAt":1614355593,"executionNumber":1}}],"nextToken":"token"}"""
            )
        )
        val listed = clientFor(mock)
            .listJobExecutionsForThing(thingName, status = QUEUED, maxResults = 5, nextToken = "from")
            .successValue()

        assertThat(mock.request!!, hasMethod(GET))
        assertThat(
            mock.request!!,
            hasUri(uri("/things/my-thing/jobs?maxResults=5&nextToken=from&status=QUEUED"))
        )

        assertThat(listed.executionSummaries.map { it.jobId }, equalTo(listOf(jobId)))
        assertThat(listed.executionSummaries[0].jobExecutionSummary.status, equalTo(QUEUED))
        assertThat(listed.nextToken, equalTo("token"))
    }

    @Test
    fun `cancel job builds the documented request`() {
        iot.cancelJob(jobId, comment = "why not", force = true).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(PUT))
        assertThat(request, hasUri(uri("/jobs/my-job/cancel?force=true")))
        assertThat(request, hasBody("""{"comment":"why not"}"""))
    }

    @Test
    fun `cancel job omits every optional field when not set`() {
        iot.cancelJob(jobId).successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/jobs/my-job/cancel")))
        assertThat(mockHttp.request!!, hasBody("{}"))
    }

    @Test
    fun `delete job builds the documented request`() {
        val mock = MockHttp(Response(OK))

        clientFor(mock).deleteJob(jobId, force = true).successValue()

        assertThat(mock.request!!, hasMethod(DELETE))
        assertThat(mock.request!!, hasUri(uri("/jobs/my-job?force=true")))
    }

    @Test
    fun `delete job omits the force flag when not set`() {
        val mock = MockHttp(Response(OK))

        clientFor(mock).deleteJob(jobId).successValue()

        assertThat(mock.request!!, hasUri(uri("/jobs/my-job")))
    }

    @Test
    fun `describe endpoint builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body("""{"endpointAddress":"abc123-ats.iot.us-east-1.amazonaws.com"}"""))
        val endpoint = clientFor(mock).describeEndpoint("iot:Data-ATS").successValue()

        assertThat(mock.request!!, hasMethod(GET))
        assertThat(mock.request!!, hasUri(uri("/endpoint?endpointType=iot%3AData-ATS")))
        assertThat(endpoint.endpointAddress, equalTo("abc123-ats.iot.us-east-1.amazonaws.com"))
    }

    @Test
    fun `describe endpoint omits the endpoint type when not set`() {
        val mock = MockHttp(Response(OK).body("""{"endpointAddress":"abc123-ats.iot.us-east-1.amazonaws.com"}"""))

        clientFor(mock).describeEndpoint().successValue()

        assertThat(mock.request!!, hasUri(uri("/endpoint")))
    }

    /** A colon is the one character the thing-name charset allows which a path must still escape. */
    @Test
    fun `action leaves percent-encoding of the thing name to the signing filter`() {
        assertThat(
            DescribeJobExecution(ThingName.of("thing:one"), jobId).toRequest().uri.path,
            equalTo("/things/thing:one/jobs/my-job")
        )

        val mock = MockHttp(Response(OK).body(DESCRIBED_EXECUTION))

        clientFor(mock).describeJobExecution(ThingName.of("thing:one"), jobId).successValue()

        assertThat(mock.request!!.uri.path, equalTo("/things/thing%3Aone/jobs/my-job"))
    }

    /** A slashed or spaced value would build a path AWS rejects and the fake cannot route. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["", " ", "job one", "job/one", "job.one", "job+one", "job\$next"])
    fun `job ids and thing names outside the AWS charset are rejected`(name: String) {
        assertThrows<IllegalArgumentException> { JobId.of(name) }
        assertThrows<IllegalArgumentException> { ThingName.of(name) }
    }

    @Test
    fun `job ids and thing names over the AWS length limit are rejected`() {
        JobId.of("j".repeat(64))
        ThingName.of("t".repeat(128))

        assertThrows<IllegalArgumentException> { JobId.of("j".repeat(65)) }
        assertThrows<IllegalArgumentException> { ThingName.of("t".repeat(129)) }
    }

    /** POST, unlike CreateJob's PUT - the two differ in the service model. */
    @Test
    fun `create stream builds the documented request`() {
        val http = MockHttp(Response(OK).body(CREATED_STREAM))
        val created = clientFor(http).createStream(
            streamId = streamId,
            files = listOf(StreamFile(fileId = 0, s3Location = S3Location("my-bucket", "image.bin", "v2"))),
            roleArn = roleArn,
            description = "a stream",
        ).successValue()

        val request = http.request!!

        assertThat(request, hasMethod(POST))
        assertThat(request, hasUri(uri("/streams/my-stream")))
        assertThat(request, hasHeader("Content-Type", "application/json; charset=utf-8"))
        assertThat(
            request, hasBody(
                """{"files":[{"fileId":0,"s3Location":{"bucket":"my-bucket","key":"image.bin","version":"v2"}}],""" +
                    """"roleArn":"${roleArn.value}","description":"a stream"}"""
            )
        )
        assertThat(created.streamId, equalTo(streamId))
        assertThat(created.streamVersion, equalTo(0))
    }

    @Test
    fun `describe stream unmarshals the stream info`() {
        val http = MockHttp(Response(OK).body(DESCRIBED_STREAM))
        val info = clientFor(http).describeStream(streamId).successValue().streamInfo

        assertThat(http.request!!, hasMethod(GET))
        assertThat(http.request!!, hasUri(uri("/streams/my-stream")))
        assertThat(info.streamId, equalTo(streamId))
        assertThat(info.streamVersion, equalTo(3))
        assertThat(info.roleArn, equalTo(roleArn))
        assertThat(
            info.files,
            equalTo(listOf(StreamFile(fileId = 0, s3Location = S3Location("my-bucket", "image.bin"))))
        )
        assertThat(info.createdAt, equalTo(Timestamp.of(1614355593)))
    }

    @Test
    fun `update stream builds the documented request`() {
        val http = MockHttp(Response(OK).body(CREATED_STREAM))
        clientFor(http).updateStream(streamId, description = "renamed").successValue()

        val request = http.request!!

        assertThat(request, hasMethod(PUT))
        assertThat(request, hasUri(uri("/streams/my-stream")))
        assertThat(request, hasBody("""{"description":"renamed"}"""))
    }

    /** No force parameter, unlike DeleteJob - AWS does not offer one for a stream. */
    @Test
    fun `delete stream builds the documented request`() {
        val http = MockHttp(Response(OK))
        clientFor(http).deleteStream(streamId).successValue()

        assertThat(http.request!!, hasMethod(DELETE))
        assertThat(http.request!!, hasUri(uri("/streams/my-stream")))
    }

    /** The query parameter is `isAscendingOrder`, which is not what the member is called. */
    @Test
    fun `list streams builds the documented request`() {
        val http = MockHttp(Response(OK).body(LISTED_STREAMS))
        val streams = clientFor(http)
            .listStreams(maxResults = 10, nextToken = "token", ascendingOrder = true).successValue()

        val request = http.request!!

        assertThat(request, hasMethod(GET))
        assertThat(request, hasUri(uri("/streams?maxResults=10&nextToken=token&isAscendingOrder=true")))
        assertThat(streams.streams.map { it.streamId }, equalTo(listOf(streamId)))
        assertThat(streams.nextToken, equalTo("next"))
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["", " ", "stream one", "stream/one", "stream.one", "stream+one"])
    fun `stream ids outside the AWS charset are rejected`(name: String) {
        assertThrows<IllegalArgumentException> { StreamId.of(name) }
    }

    @Test
    fun `stream ids over the AWS length limit are rejected`() {
        StreamId.of("s".repeat(128))

        assertThrows<IllegalArgumentException> { StreamId.of("s".repeat(129)) }
    }
}

private val CREATED_JOB =
    """{"jobArn":"arn:aws:iot:us-east-1:000000000000:job/my-job","jobId":"my-job","description":"a job"}"""

private val DESCRIBED_JOB =
    """{"job":{"jobArn":"arn:aws:iot:us-east-1:000000000000:job/my-job","jobId":"my-job",""" +
        """"targets":["arn:aws:iot:us-east-1:000000000000:thing/my-thing"],"targetSelection":"SNAPSHOT",""" +
        """"status":"IN_PROGRESS","createdAt":1614355593,"lastUpdatedAt":1614355593,""" +
        """"timeoutConfig":{"inProgressTimeoutInMinutes":5}}}"""

private val CREATED_STREAM =
    """{"streamId":"my-stream","streamArn":"arn:aws:iot:us-east-1:000000000000:stream/my-stream",""" +
        """"streamVersion":0,"description":"a stream"}"""

private val DESCRIBED_STREAM =
    """{"streamInfo":{"streamId":"my-stream",""" +
        """"streamArn":"arn:aws:iot:us-east-1:000000000000:stream/my-stream","streamVersion":3,""" +
        """"files":[{"fileId":0,"s3Location":{"bucket":"my-bucket","key":"image.bin"}}],""" +
        """"roleArn":"arn:aws:iam::000000000000:role/my-stream-role","createdAt":1614355593,""" +
        """"lastUpdatedAt":1614355593}}"""

private val LISTED_STREAMS =
    """{"streams":[{"streamId":"my-stream",""" +
        """"streamArn":"arn:aws:iot:us-east-1:000000000000:stream/my-stream","streamVersion":3}],""" +
        """"nextToken":"next"}"""

private val DESCRIBED_EXECUTION =
    """{"execution":{"jobId":"my-job","status":"QUEUED","statusDetails":{"detailsMap":{"step":"queued"}},""" +
        """"thingArn":"arn:aws:iot:us-east-1:000000000000:thing/my-thing","queuedAt":1614355593,""" +
        """"lastUpdatedAt":1614355593,"executionNumber":1,"versionNumber":1}}"""
