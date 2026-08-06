package org.http4k.connect.amazon.iotjobsdataplane

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iotjobsdataplane.action.DescribeJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.action.StartNextPendingJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.IN_PROGRESS
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.QUEUED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus.SUCCEEDED
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
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

class HttpIotJobsDataPlaneTest {

    private val thingName = ThingName.of("my-thing")
    private val jobId = JobId.of("my-job")

    private val mockHttp = MockHttp(Response(OK).body("{}"))

    private val iotJobsDataPlane = clientFor(mockHttp)

    private fun clientFor(http: MockHttp) =
        IotJobsDataPlane.Http(Region.US_EAST_1, CredentialsProvider.FakeAwsEnvironment(), http)

    private fun uri(path: String) = Uri.of("https://data.jobs.iot.us-east-1.amazonaws.com$path")

    @Test
    fun `get pending job executions builds the documented request`() {
        iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()

        assertThat(mockHttp.request!!, hasMethod(GET))
        assertThat(mockHttp.request!!, hasUri(uri("/things/my-thing/jobs")))
    }

    @Test
    fun `get pending job executions unmarshals the response document`() {
        val pending = clientFor(
            MockHttp(
                Response(OK).body(
                    """{"inProgressJobs":[{"jobId":"job-a","queuedAt":1614355593,"startedAt":1614355600,""" +
                        """"lastUpdatedAt":1614355600,"versionNumber":2,"executionNumber":1}],""" +
                        """"queuedJobs":[{"jobId":"job-b","queuedAt":1614355593,"versionNumber":1,"executionNumber":1}]}"""
                )
            )
        ).getPendingJobExecutions(thingName).successValue()

        assertThat(pending.inProgressJobs.map { it.jobId }, equalTo(listOf(JobId.of("job-a"))))
        assertThat(pending.inProgressJobs[0].startedAt, equalTo(Timestamp.of(1614355600)))
        assertThat(pending.inProgressJobs[0].versionNumber, equalTo(2L))
        assertThat(pending.queuedJobs.map { it.jobId }, equalTo(listOf(JobId.of("job-b"))))
        assertThat(pending.queuedJobs[0].startedAt, equalTo(null))
    }

    /**
     * The signing name (`iot-jobs-data`) differs from the endpoint host, so both halves are
     * pinned: host from the region, credential scope from the companion.
     */
    @Test
    fun `requests are signed with the iot-jobs-data service name against the data endpoint`() {
        iotJobsDataPlane.getPendingJobExecutions(thingName).successValue()

        val request = mockHttp.request!!

        assertThat(request.uri.host, equalTo("data.jobs.iot.us-east-1.amazonaws.com"))
        assertThat(request.header("Authorization")!!, containsSubstring("/us-east-1/iot-jobs-data/aws4_request"))
    }

    @Test
    fun `the endpoint can be overridden without changing the signing name`() {
        val mock = MockHttp(Response(OK).body("{}"))

        IotJobsDataPlane.Http(
            Region.US_EAST_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mock,
            overrideEndpoint = Uri.of("http://localhost:12345")
        ).getPendingJobExecutions(thingName).successValue()

        assertThat(mock.request!!.uri.host, equalTo("localhost"))
        assertThat(mock.request!!.header("Authorization")!!, containsSubstring("/us-east-1/iot-jobs-data/aws4_request"))
    }

    @Test
    fun `describe job execution builds the documented request`() {
        iotJobsDataPlane.describeJobExecution(thingName, jobId, executionNumber = 2, includeJobDocument = false)
            .successValue()

        assertThat(mockHttp.request!!, hasMethod(GET))
        assertThat(
            mockHttp.request!!,
            hasUri(uri("/things/my-thing/jobs/my-job?executionNumber=2&includeJobDocument=false"))
        )
    }

    /**
     * includeJobDocument is left to the server default (true, per the AWS API reference) by
     * omitting the query entirely when the parameter is not given.
     */
    @Test
    fun `describe job execution omits both optional query parameters when not set`() {
        iotJobsDataPlane.describeJobExecution(thingName, jobId).successValue()

        assertThat(mockHttp.request!!, hasUri(uri("/things/my-thing/jobs/my-job")))
    }

    @Test
    fun `describe job execution unmarshals the response document`() {
        val execution = clientFor(MockHttp(Response(OK).body(DESCRIBED_EXECUTION)))
            .describeJobExecution(thingName, JobId.NEXT).successValue().execution!!

        assertThat(execution.jobId, equalTo(jobId))
        assertThat(execution.thingName, equalTo(thingName))
        assertThat(execution.status, equalTo(QUEUED))
        assertThat(execution.statusDetails, equalTo(mapOf("step" to "queued")))
        assertThat(execution.queuedAt, equalTo(Timestamp.of(1614355593)))
        assertThat(execution.versionNumber, equalTo(1L))
        assertThat(execution.jobDocument, equalTo("""{"operation":"noop"}"""))
    }

    /** With nothing pending the service answers 200 with an empty document, not an error. */
    @Test
    fun `describe job execution treats an empty response as no execution`() {
        assertThat(iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue().execution, equalTo(null))
    }

    /**
     * The action leaves the `$next` segment unencoded, because the AWS auth filter encodes
     * the path afterwards - encoding it here as well would put %2524 on the wire.
     */
    @Test
    fun `action leaves percent-encoding of the next literal to the signing filter`() {
        assertThat(
            DescribeJobExecution(thingName, JobId.NEXT).toRequest().uri.path,
            equalTo("/things/my-thing/jobs/\$next")
        )
        assertThat(
            StartNextPendingJobExecution(thingName).toRequest().uri.path,
            equalTo("/things/my-thing/jobs/\$next")
        )

        iotJobsDataPlane.describeJobExecution(thingName, JobId.NEXT).successValue()

        assertThat(mockHttp.request!!.uri.path, equalTo("/things/my-thing/jobs/%24next"))
    }

    @Test
    fun `start next pending job execution builds the documented request`() {
        iotJobsDataPlane.startNextPendingJobExecution(
            thingName,
            statusDetails = mapOf("step" to "downloading"),
            stepTimeoutInMinutes = 10,
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(PUT))
        assertThat(request.uri.path, equalTo("/things/my-thing/jobs/%24next"))
        assertThat(request, hasHeader("Content-Type", "application/json; charset=utf-8"))
        assertThat(request, hasBody("""{"statusDetails":{"step":"downloading"},"stepTimeoutInMinutes":10}"""))
    }

    @Test
    fun `start next pending job execution sends an empty document when nothing is set`() {
        iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue()

        assertThat(mockHttp.request!!, hasBody("{}"))
    }

    @Test
    fun `start next pending job execution treats an empty response as no execution`() {
        assertThat(iotJobsDataPlane.startNextPendingJobExecution(thingName).successValue().execution, equalTo(null))
    }

    @Test
    fun `update job execution builds the documented request`() {
        iotJobsDataPlane.updateJobExecution(
            thingName, jobId, SUCCEEDED,
            statusDetails = mapOf("firmware" to "confirmed"),
            expectedVersion = 3,
            includeJobExecutionState = true,
            includeJobDocument = true,
            executionNumber = 1,
            stepTimeoutInMinutes = -1,
        ).successValue()

        val request = mockHttp.request!!

        assertThat(request, hasMethod(POST))
        assertThat(request, hasUri(uri("/things/my-thing/jobs/my-job")))
        assertThat(
            request, hasBody(
                """{"status":"SUCCEEDED","statusDetails":{"firmware":"confirmed"},"expectedVersion":3,""" +
                    """"includeJobExecutionState":true,"includeJobDocument":true,"executionNumber":1,""" +
                    """"stepTimeoutInMinutes":-1}"""
            )
        )
    }

    @Test
    fun `update job execution omits every optional field when not set`() {
        iotJobsDataPlane.updateJobExecution(thingName, jobId, IN_PROGRESS).successValue()

        assertThat(mockHttp.request!!, hasBody("""{"status":"IN_PROGRESS"}"""))
    }

    @Test
    fun `update job execution unmarshals the response document`() {
        val updated = clientFor(
            MockHttp(
                Response(OK).body(
                    """{"executionState":{"status":"SUCCEEDED","statusDetails":{"firmware":"confirmed"},""" +
                        """"versionNumber":4},"jobDocument":"{\"operation\":\"noop\"}"}"""
                )
            )
        ).updateJobExecution(thingName, jobId, SUCCEEDED).successValue()

        val executionState = updated.executionState!!
        assertThat(executionState.status, equalTo(SUCCEEDED))
        assertThat(executionState.versionNumber, equalTo(4L))
        assertThat(executionState.statusDetails, equalTo(mapOf("firmware" to "confirmed")))
        assertThat(updated.jobDocument, equalTo("""{"operation":"noop"}"""))
    }

    @Test
    fun `the next literal is a valid job id`() {
        assertThat(JobId.NEXT.value, equalTo("\$next"))
        assertThat(JobId.of("\$next"), equalTo(JobId.NEXT))
    }

    /** Only the exact literal is reserved; anything else containing a dollar stays invalid. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["", " ", "job one", "job/one", "\$nextish", "job\$next", "\$NEXT"])
    fun `job ids outside the AWS charset are rejected`(name: String) {
        assertThrows<IllegalArgumentException> { JobId.of(name) }
    }

    @Test
    fun `thing names outside the AWS charset are rejected`() {
        ThingName.of("t".repeat(128))

        assertThrows<IllegalArgumentException> { ThingName.of("t".repeat(129)) }
        assertThrows<IllegalArgumentException> { ThingName.of("thing/one") }
        assertThrows<IllegalArgumentException> { ThingName.of("") }
    }
}

private val DESCRIBED_EXECUTION =
    """{"execution":{"jobId":"my-job","thingName":"my-thing","status":"QUEUED",""" +
        """"statusDetails":{"step":"queued"},"queuedAt":1614355593,"lastUpdatedAt":1614355593,""" +
        """"versionNumber":1,"executionNumber":1,"jobDocument":"{\"operation\":\"noop\"}"}}"""
