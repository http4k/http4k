package org.http4k.connect.amazon.iot

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.action.CancelJobData
import org.http4k.connect.amazon.iot.action.CancelledJob
import org.http4k.connect.amazon.iot.action.CreateJobData
import org.http4k.connect.amazon.iot.action.CreateStreamData
import org.http4k.connect.amazon.iot.action.CreatedJob
import org.http4k.connect.amazon.iot.action.CreatedStream
import org.http4k.connect.amazon.iot.action.DescribedJob
import org.http4k.connect.amazon.iot.action.DescribedJobExecution
import org.http4k.connect.amazon.iot.action.DescribedStream
import org.http4k.connect.amazon.iot.action.IotEndpoint
import org.http4k.connect.amazon.iot.action.Job
import org.http4k.connect.amazon.iot.action.JobExecution
import org.http4k.connect.amazon.iot.action.JobExecutionStatusDetails
import org.http4k.connect.amazon.iot.action.JobExecutionSummary
import org.http4k.connect.amazon.iot.action.JobExecutionSummaryForThing
import org.http4k.connect.amazon.iot.action.JobExecutionsForThing
import org.http4k.connect.amazon.iot.action.StreamInfo
import org.http4k.connect.amazon.iot.action.StreamSummary
import org.http4k.connect.amazon.iot.action.Streams
import org.http4k.connect.amazon.iot.action.UpdateStreamData
import org.http4k.connect.amazon.iot.action.UpdatedStream
import org.http4k.connect.amazon.iot.model.JobExecutionStatus
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.TargetSelection
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock

/**
 * The AWS IoT control plane, of which the Jobs and stream operations are implemented. Share the
 * [jobs] storage with a FakeIotJobsDataPlane and the control plane and the device API see
 * one jobs state.
 */
class FakeIot(
    private val jobs: Storage<StoredJob> = Storage.InMemory(),
    private val streams: Storage<StoredStream> = Storage.InMemory(),
    private val region: Region = Region.of("ldn-north-1"),
    private val clock: Clock = Clock.systemUTC(),
) : ChaoticHttpHandler() {

    override val app = routes(
        "/jobs/{jobId}/cancel" bind PUT to ::cancelJob,
        "/jobs/{jobId}" bind PUT to ::createJob,
        "/jobs/{jobId}" bind GET to ::describeJob,
        "/jobs/{jobId}" bind DELETE to ::deleteJob,
        "/things/{thingName}/jobs/{jobId}" bind GET to ::describeJobExecution,
        "/things/{thingName}/jobs" bind GET to ::listJobExecutionsForThing,
        "/streams/{streamId}" bind POST to ::createStream,
        "/streams/{streamId}" bind GET to ::describeStream,
        "/streams/{streamId}" bind PUT to ::updateStream,
        "/streams/{streamId}" bind DELETE to ::deleteStream,
        "/streams" bind GET to ::listStreams,
        "/endpoint" bind GET to ::describeEndpoint,
    )

    /**
     * One QUEUED execution per `thing/NAME` target. A `thinggroup/NAME` target is accepted and
     * produces no executions: group membership is not modelled, and reading the group's name as
     * a thing name would invent a device that could then claim the job.
     */
    private fun createJob(request: Request): Response {
        val jobId = JobId.of(request.path("jobId")!!)

        val data = IotMoshi.asA<CreateJobData>(request.bodyString().ifEmpty { "{}" })

        if (data.targets.isEmpty()) return invalidRequest("targets must not be empty")

        val document = data.document
            ?: return invalidRequest("document must be specified: documentSource is not supported")

        val now = Timestamp.of(clock.instant())
        val job = StoredJob(
            jobId = jobId,
            jobArn = ARN.of("arn:aws:iot:$region:$ACCOUNT:job/${jobId.value}"),
            targets = data.targets,
            document = document,
            description = data.description,
            targetSelection = data.targetSelection ?: TargetSelection.SNAPSHOT,
            status = JobStatus.IN_PROGRESS,
            comment = null,
            forceCanceled = null,
            timeoutConfig = data.timeoutConfig,
            createdAt = now,
            lastUpdatedAt = now,
            completedAt = null,
            executions = data.targets.mapNotNull { target ->
                target.thingNameOrNull()?.let { thingName ->
                    thingName to StoredJobExecution(
                        thingName = thingName,
                        thingArn = target,
                        status = JobExecutionStatus.QUEUED,
                        statusDetails = emptyMap(),
                        queuedAt = now,
                        startedAt = null,
                        lastUpdatedAt = now,
                        versionNumber = 1,
                        executionNumber = 1,
                    )
                }
            }.toMap(),
        )

        synchronized(jobs) {
            jobs[jobId.value]?.let { return jobAlreadyExists(it) }
            jobs[jobId.value] = job
        }

        return Response(OK).body(IotMoshi.asFormatString(CreatedJob(job.jobArn, jobId, job.description)))
    }

    private fun describeJob(request: Request): Response {
        val jobId = JobId.of(request.path("jobId")!!)
        val job = jobs[jobId.value] ?: return jobNotFound(jobId)

        return Response(OK).body(IotMoshi.asFormatString(DescribedJob(job.toJob())))
    }

    private fun describeJobExecution(request: Request): Response {
        val jobId = JobId.of(request.path("jobId")!!)
        val thingName = ThingName.of(request.path("thingName")!!)
        val job = jobs[jobId.value] ?: return jobNotFound(jobId)
        val execution = job.executions[thingName] ?: return executionNotFound(jobId)
        val requested = request.query("executionNumber")
        val executionNumber = requested?.toLongOrNull()

        return when {
            requested != null && executionNumber == null -> invalidRequest("executionNumber must be a number")
            executionNumber != null && executionNumber != execution.executionNumber -> executionNotFound(jobId)
            else -> describedExecution(job, execution)
        }
    }

    private fun describedExecution(job: StoredJob, execution: StoredJobExecution) = Response(OK).body(
        IotMoshi.asFormatString(
            DescribedJobExecution(
                JobExecution(
                    jobId = job.jobId,
                    status = execution.status,
                    statusDetails = execution.statusDetails
                        .takeIf { it.isNotEmpty() }
                        ?.let { JobExecutionStatusDetails(it) },
                    thingArn = execution.thingArn,
                    queuedAt = execution.queuedAt,
                    startedAt = execution.startedAt,
                    lastUpdatedAt = execution.lastUpdatedAt,
                    executionNumber = execution.executionNumber,
                    versionNumber = execution.versionNumber,
                    forceCanceled = execution.forceCanceled,
                    approximateSecondsBeforeTimedOut = job.approximateSecondsBeforeTimedOut(execution),
                )
            )
        )
    )

    private fun listJobExecutionsForThing(request: Request): Response {
        val thingName = ThingName.of(request.path("thingName")!!)
        val statusFilter = request.query("status")?.let { raw ->
            JobExecutionStatus.entries.firstOrNull { it.name == raw }
                ?: return invalidRequest("$raw is not a job execution status")
        }
        val jobIdFilter = request.query("jobId")?.let { raw ->
            runCatching { JobId.of(raw) }.getOrNull() ?: return invalidRequest("$raw is not a job id")
        }

        return pageOfExecutions(
            jobs.executionsFor(thingName)
                .filter { (job, _) -> jobIdFilter == null || job.jobId == jobIdFilter }
                .filter { (_, execution) -> statusFilter == null || execution.status == statusFilter },
            request,
        )
    }

    private fun pageOfExecutions(
        summaries: List<Pair<StoredJob, StoredJobExecution>>,
        request: Request,
    ): Response {
        val from = request.query("nextToken")
            ?.let { it.toIntOrNull() ?: return invalidRequest("nextToken must be a number") } ?: 0
        val maxResults = request.query("maxResults")
            ?.let { it.toIntOrNull() ?: return invalidRequest("maxResults must be a number") } ?: summaries.size
        val page = summaries.drop(from).take(maxResults)

        return Response(OK).body(
            IotMoshi.asFormatString(
                JobExecutionsForThing(
                    executionSummaries = page.map { (job, execution) ->
                        JobExecutionSummaryForThing(
                            jobId = job.jobId,
                            jobExecutionSummary = JobExecutionSummary(
                                status = execution.status,
                                queuedAt = execution.queuedAt,
                                startedAt = execution.startedAt,
                                lastUpdatedAt = execution.lastUpdatedAt,
                                executionNumber = execution.executionNumber,
                            ),
                        )
                    },
                    nextToken = (from + page.size).takeIf { it < summaries.size }?.toString(),
                )
            )
        )
    }

    /**
     * Only an IN_PROGRESS job can be canceled. QUEUED executions are canceled; IN_PROGRESS
     * executions only with force=true, otherwise they are left for the device to finish.
     */
    private fun cancelJob(request: Request): Response {
        val jobId = JobId.of(request.path("jobId")!!)
        val force = request.query("force")
            ?.let { it.toBooleanStrictOrNull() ?: return invalidRequest("force must be true or false") } ?: false
        val data = IotMoshi.asA<CancelJobData>(request.bodyString().ifEmpty { "{}" })

        synchronized(jobs) {
            val job = jobs[jobId.value] ?: return jobNotFound(jobId)

            if (job.status != JobStatus.IN_PROGRESS) {
                return invalidRequest("Job ${jobId.value} is in status ${job.status} and cannot be canceled")
            }

            val now = Timestamp.of(clock.instant())

            jobs[jobId.value] = job.copy(
                status = JobStatus.CANCELED,
                comment = data.comment,
                forceCanceled = force.takeIf { it },
                lastUpdatedAt = now,
                executions = job.executions.mapValues { (_, execution) ->
                    when {
                        execution.status == JobExecutionStatus.QUEUED ||
                            (force && execution.status == JobExecutionStatus.IN_PROGRESS) -> execution.copy(
                            status = JobExecutionStatus.CANCELED,
                            lastUpdatedAt = now,
                            versionNumber = execution.versionNumber + 1,
                            // Only the execution force actually took down carries the flag: a
                            // QUEUED one would have been canceled either way.
                            forceCanceled = true.takeIf {
                                force && execution.status == JobExecutionStatus.IN_PROGRESS
                            },
                        )

                        else -> execution
                    }
                },
            )

            return Response(OK).body(IotMoshi.asFormatString(CancelledJob(job.jobArn, jobId, job.description)))
        }
    }

    /**
     * A non-terminal job is refused without force. Deletion is instant in the fake: there is
     * no DELETION_IN_PROGRESS window.
     */
    private fun deleteJob(request: Request): Response {
        val jobId = JobId.of(request.path("jobId")!!)
        val force = request.query("force")
            ?.let { it.toBooleanStrictOrNull() ?: return invalidRequest("force must be true or false") } ?: false

        synchronized(jobs) {
            val job = jobs[jobId.value] ?: return jobNotFound(jobId)
            val terminal = job.status == JobStatus.COMPLETED || job.status == JobStatus.CANCELED

            if (!terminal && !force) {
                return invalidStateTransition("Job ${jobId.value} is in status ${job.status} and cannot be deleted without force")
            }

            jobs.remove(jobId.value)
        }

        return Response(OK)
    }

    /**
     * A stream carries 1 to 50 files and a role for IoT to read them with, both required. The
     * fake records where the bytes were said to be and never reads them: devices fetch blocks
     * over MQTT, which this HTTP API has no part in.
     */
    private fun createStream(request: Request): Response {
        val streamId = StreamId.of(request.path("streamId")!!)

        val data = IotMoshi.asA<CreateStreamData>(request.bodyString().ifEmpty { "{}" })

        invalidFiles(data.files)?.let { return it }
        val roleArn = data.roleArn ?: return invalidRequest("roleArn must be specified")

        val now = Timestamp.of(clock.instant())
        val stream = StoredStream(
            streamId = streamId,
            streamArn = ARN.of("arn:aws:iot:$region:$ACCOUNT:stream/${streamId.value}"),
            streamVersion = 0,
            files = data.files,
            roleArn = roleArn,
            description = data.description,
            createdAt = now,
            lastUpdatedAt = now,
        )

        synchronized(streams) {
            streams[streamId.value]?.let { return streamAlreadyExists(it) }
            streams[streamId.value] = stream
        }

        return Response(OK).body(
            IotMoshi.asFormatString(
                CreatedStream(streamId, stream.streamArn, stream.streamVersion, stream.description)
            )
        )
    }

    private fun describeStream(request: Request): Response {
        val streamId = StreamId.of(request.path("streamId")!!)
        val stream = streams[streamId.value] ?: return streamNotFound(streamId)

        return Response(OK).body(
            IotMoshi.asFormatString(
                DescribedStream(
                    StreamInfo(
                        streamId = stream.streamId,
                        streamArn = stream.streamArn,
                        streamVersion = stream.streamVersion,
                        files = stream.files,
                        roleArn = stream.roleArn,
                        description = stream.description,
                        createdAt = stream.createdAt,
                        lastUpdatedAt = stream.lastUpdatedAt,
                    )
                )
            )
        )
    }

    /** Each update bumps the version, which is what a device sees change under it. */
    private fun updateStream(request: Request): Response {
        val streamId = StreamId.of(request.path("streamId")!!)
        val data = IotMoshi.asA<UpdateStreamData>(request.bodyString().ifEmpty { "{}" })

        data.files?.let { files -> invalidFiles(files)?.let { return it } }

        synchronized(streams) {
            val stream = streams[streamId.value] ?: return streamNotFound(streamId)

            val updated = stream.copy(
                streamVersion = stream.streamVersion + 1,
                files = data.files ?: stream.files,
                roleArn = data.roleArn ?: stream.roleArn,
                description = data.description ?: stream.description,
                lastUpdatedAt = Timestamp.of(clock.instant()),
            )
            streams[streamId.value] = updated

            return Response(OK).body(
                IotMoshi.asFormatString(
                    UpdatedStream(streamId, updated.streamArn, updated.streamVersion, updated.description)
                )
            )
        }
    }

    /**
     * Whether a stream is still referenced by a job is not modelled, so a delete here always
     * succeeds where AWS may answer DeleteConflictException. The association lives in AWS's own
     * bookkeeping rather than in anything this API returns, so the fake would be guessing at it.
     */
    private fun deleteStream(request: Request): Response {
        val streamId = StreamId.of(request.path("streamId")!!)

        synchronized(streams) {
            streams[streamId.value] ?: return streamNotFound(streamId)
            streams.remove(streamId.value)
        }

        return Response(OK)
    }

    private fun listStreams(request: Request): Response {
        val ascending = request.query("isAscendingOrder")
            ?.let { it.toBooleanStrictOrNull() ?: return invalidRequest("isAscendingOrder must be true or false") } ?: false
        val all = streams.keySet()
            .mapNotNull { streams[it] }
            .sortedWith(compareBy({ it.createdAt.value }, { it.streamId.value }))
            .let { if (ascending) it else it.reversed() }

        val from = request.query("nextToken")
            ?.let { it.toIntOrNull() ?: return invalidRequest("nextToken must be a number") } ?: 0
        val page = all.drop(from).take(request.query("maxResults")
            ?.let { it.toIntOrNull() ?: return invalidRequest("maxResults must be a number") } ?: all.size)

        return Response(OK).body(
            IotMoshi.asFormatString(
                Streams(
                    streams = page.map {
                        StreamSummary(it.streamId, it.streamArn, it.streamVersion, it.description)
                    },
                    nextToken = (from + page.size).takeIf { it < all.size }?.toString(),
                )
            )
        )
    }

    /**
     * `iot:Jobs` is refused, in the words AWS itself uses: the Jobs APIs moved to the
     * `iot:Data-ATS` endpoint and the dedicated Jobs endpoint type is no longer served.
     * Verified against a real account on 2026-08-10.
     */
    private fun describeEndpoint(request: Request): Response {
        val address = when (request.query("endpointType")) {
            null, "iot:Data-ATS" -> "$ENDPOINT_IDENTIFIER-ats.iot.$region.amazonaws.com"

            "iot:Data" -> "$ENDPOINT_IDENTIFIER.iot.$region.amazonaws.com"

            "iot:CredentialProvider" -> "$ENDPOINT_IDENTIFIER.credentials.iot.$region.amazonaws.com"

            "iot:Jobs" -> return invalidRequest(
                "IoT Jobs and Commands APIs are now available through iot:Data-ATS endpoints " +
                    "instead of iot:Jobs endpoints. Please use iot:Data-ATS."
            )

            else -> return invalidRequest("Invalid endpoint type")
        }

        return Response(OK).body(IotMoshi.asFormatString(IotEndpoint(address)))
    }

    /** The stream-file constraints AWS enforces, applied by both create and update. */
    private fun invalidFiles(files: List<StreamFile>) = when {
        files.isEmpty() -> invalidRequest("files must not be empty")

        files.size > MAX_STREAM_FILES -> invalidRequest("a stream carries at most $MAX_STREAM_FILES files")

        else -> files.firstOrNull { it.fileId !in 0..MAX_FILE_ID }
            ?.let { invalidRequest("fileId ${it.fileId} is outside 0..$MAX_FILE_ID") }
    }

    private fun StoredJob.toJob() = Job(
        jobArn = jobArn,
        jobId = jobId,
        targets = targets,
        targetSelection = targetSelection,
        status = status,
        forceCanceled = forceCanceled,
        comment = comment,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
        completedAt = completedAt,
        description = description,
        timeoutConfig = timeoutConfig,
    )

    /**
     * Convenience function to get an Iot client
     */
    fun client() = Iot.Http(region, { AwsCredentials("accessKey", "secret") }, this)

    fun job(jobId: JobId) = jobs[jobId.value]

    fun stream(streamId: StreamId) = streams[streamId.value]

    companion object {
        private const val ACCOUNT = "000000000000"
        private const val ENDPOINT_IDENTIFIER = "http4k000000"
        private const val MAX_STREAM_FILES = 50
        private const val MAX_FILE_ID = 255
    }
}

fun main() {
    FakeIot().start()
}
