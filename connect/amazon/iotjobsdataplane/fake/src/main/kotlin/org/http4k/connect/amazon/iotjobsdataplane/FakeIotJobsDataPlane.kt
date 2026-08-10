package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iot.StoredJob
import org.http4k.connect.amazon.iot.StoredJobExecution
import org.http4k.connect.amazon.iot.approximateSecondsBeforeTimedOut
import org.http4k.connect.amazon.iot.completedIfAllExecutionsTerminal
import org.http4k.connect.amazon.iot.executionsFor
import org.http4k.connect.amazon.iot.isTerminal
import org.http4k.connect.amazon.iot.nextPendingExecutionFor
import org.http4k.connect.amazon.iotjobsdataplane.action.DescribedJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.action.JobExecutionState
import org.http4k.connect.amazon.iotjobsdataplane.action.JobExecutionSummary
import org.http4k.connect.amazon.iotjobsdataplane.action.PendingJobExecutions
import org.http4k.connect.amazon.iotjobsdataplane.action.StartNextPendingJobExecutionData
import org.http4k.connect.amazon.iotjobsdataplane.action.StartedJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.action.UpdateJobExecutionData
import org.http4k.connect.amazon.iotjobsdataplane.action.UpdatedJobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock
import org.http4k.connect.amazon.iot.model.JobExecutionStatus as StoredExecutionStatus
import org.http4k.connect.amazon.iot.model.ThingName as StoredThingName

private val thingNameLens = Path.value(StoredThingName).of("thingName")

/**
 * The device side of AWS IoT Jobs. Construct it over the same [jobs] storage as a FakeIot
 * and the control plane and the device API see one jobs state; on its own it starts empty
 * (and, having no CreateJob, stays that way unless the storage is seeded directly).
 */
class FakeIotJobsDataPlane(
    private val jobs: Storage<StoredJob> = Storage.InMemory(),
    private val region: Region = Region.of("ldn-north-1"),
    private val clock: Clock = Clock.systemUTC(),
) : ChaoticHttpHandler() {

    /**
     * A thing name outside the AWS charset is a 400 there, so the lens failure it trips here
     * becomes one too, rather than escaping the handler as a 500. The jobId stays a raw string:
     * `$next` is a legal one, and it is only ever compared or used as a store key.
     */
    override val app = CatchLensFailure { invalidRequest(it.failures.joinToString("; ")) }.then(
        routes(
            "/things/{thingName}/jobs" bind GET to ::getPendingJobExecutions,
            "/things/{thingName}/jobs/{jobId}" bind GET to ::describeJobExecution,
            "/things/{thingName}/jobs/{jobId}" bind PUT to ::startNextPendingJobExecution,
            "/things/{thingName}/jobs/{jobId}" bind POST to ::updateJobExecution,
        )
    )

    private fun getPendingJobExecutions(request: Request): Response {
        val pending = jobs.executionsFor(request.storedThingName())
            .filterNot { (_, execution) -> execution.status.isTerminal }

        return Response(OK).body(
            IotJobsDataPlaneMoshi.asFormatString(
                PendingJobExecutions(
                    inProgressJobs = pending
                        .filter { it.second.status == StoredExecutionStatus.IN_PROGRESS }
                        .map { (job, execution) -> execution.toSummary(job) },
                    queuedJobs = pending
                        .filter { it.second.status == StoredExecutionStatus.QUEUED }
                        .map { (job, execution) -> execution.toSummary(job) },
                )
            )
        )
    }

    /**
     * Read-only, even for `$next`: a device polls this on every connect precisely because it
     * is side-effect free, so no state transitions here. Describing a terminal execution by
     * its explicit jobId answers TerminalStateException (410), which the docs imply by
     * declaring that error for this operation; `$next` never selects a terminal execution.
     */
    private fun describeJobExecution(request: Request): Response {
        val thingName = request.storedThingName()
        val jobId = request.path("jobId")!!

        // the AWS API reference: "Unless set to false, the response contains the job document. The default is true."
        val includeJobDocument = request.query("includeJobDocument")
            ?.let { it.toBooleanStrictOrNull() ?: return invalidRequest("includeJobDocument must be true or false") }
            ?: true

        val executionNumber = request.query("executionNumber")
            ?.let { it.toLongOrNull() ?: return invalidRequest("executionNumber must be a number") }

        return when (jobId) {
            NEXT -> describeNextExecution(thingName, includeJobDocument)
            else -> describeExecutionById(thingName, jobId, executionNumber, includeJobDocument)
        }
    }

    private fun describeNextExecution(thingName: StoredThingName, includeJobDocument: Boolean): Response {
        val (job, execution) = jobs.nextPendingExecutionFor(thingName) ?: return Response(OK).body("{}")

        return describedExecution(job, execution, includeJobDocument)
    }

    private fun describeExecutionById(
        thingName: StoredThingName,
        jobId: String,
        executionNumber: Long?,
        includeJobDocument: Boolean,
    ): Response {
        val job = jobs[jobId] ?: return executionNotFound(jobId)
        val execution = job.executions[thingName]
            ?.takeIf { executionNumber == null || executionNumber == it.executionNumber }
            ?: return executionNotFound(jobId)

        return when {
            execution.status.isTerminal -> terminalState(jobId)
            else -> describedExecution(job, execution, includeJobDocument)
        }
    }

    private fun describedExecution(job: StoredJob, execution: StoredJobExecution, includeJobDocument: Boolean) =
        Response(OK).body(
            IotJobsDataPlaneMoshi.asFormatString(
                DescribedJobExecution(execution.toJobExecution(job, includeJobDocument))
            )
        )

    /**
     * The same selection as `$next`, but a QUEUED pick transitions to IN_PROGRESS. A pick
     * that is already IN_PROGRESS is returned untouched: per the AWS docs its status details
     * are not changed, so neither is its version. With nothing pending the response is an
     * empty document, as the docs specify. The step timeout is accepted in the shape but
     * starts no timer: the fake has no live timers.
     */
    private fun startNextPendingJobExecution(request: Request): Response {
        if (request.path("jobId") != NEXT) return invalidRequest("Only \$next can be started")

        val data = IotJobsDataPlaneMoshi.asA<StartNextPendingJobExecutionData>(request.bodyString().ifEmpty { "{}" })

        synchronized(jobs) {
            val (job, execution) = jobs.nextPendingExecutionFor(request.storedThingName())
                ?: return Response(OK).body("{}")

            val now = Timestamp.of(clock.instant())
            val started = when (execution.status) {
                StoredExecutionStatus.QUEUED -> execution.copy(
                    status = StoredExecutionStatus.IN_PROGRESS,
                    statusDetails = data.statusDetails ?: execution.statusDetails,
                    startedAt = now,
                    lastUpdatedAt = now,
                    versionNumber = execution.versionNumber + 1,
                )

                else -> execution
            }

            if (started != execution) {
                jobs[job.jobId.value] = job.copy(executions = job.executions + (started.thingName to started))
            }

            return Response(OK).body(
                IotJobsDataPlaneMoshi.asFormatString(
                    StartedJobExecution(started.toJobExecution(job, includeJobDocument = true))
                )
            )
        }
    }

    /**
     * An expectedVersion mismatch answers 409 with a VersionMismatch message: the docs call
     * this "a VersionMismatch error" but declare no such exception for the operation, so it
     * surfaces under its only declared 409, InvalidStateTransitionException. Specified
     * statusDetails replace the stored map; absent ones leave it unchanged.
     */
    private fun updateJobExecution(request: Request): Response {
        val thingName = request.storedThingName()
        val jobId = request.path("jobId")!!
        val data = IotJobsDataPlaneMoshi.asA<UpdateJobExecutionData>(request.bodyString())
        val expectedVersion = data.expectedVersion

        synchronized(jobs) {
            val job = jobs[jobId] ?: return executionNotFound(jobId)
            val execution = job.executions[thingName]
                ?.takeIf { data.executionNumber == null || data.executionNumber == it.executionNumber }
                ?: return executionNotFound(jobId)

            return when {
                data.status !in DEVICE_SETTABLE ->
                    invalidRequest("A device can only set the status to one of $DEVICE_SETTABLE")

                execution.status.isTerminal -> invalidStateTransition(
                    "The execution of job $jobId is already in the terminal state ${execution.status}",
                    execution.toState(),
                )

                expectedVersion != null && expectedVersion != execution.versionNumber -> invalidStateTransition(
                    "VersionMismatch: expected version $expectedVersion but the execution of job $jobId " +
                        "has version ${execution.versionNumber}",
                    execution.toState(),
                )

                else -> applyUpdate(job, execution, data)
            }
        }
    }

    private fun applyUpdate(job: StoredJob, execution: StoredJobExecution, data: UpdateJobExecutionData): Response {
        val now = Timestamp.of(clock.instant())
        val newStatus = StoredExecutionStatus.valueOf(data.status.name)
        val updated = execution.copy(
            status = newStatus,
            statusDetails = data.statusDetails ?: execution.statusDetails,
            startedAt = execution.startedAt ?: now.takeIf { newStatus == StoredExecutionStatus.IN_PROGRESS },
            lastUpdatedAt = now,
            versionNumber = execution.versionNumber + 1,
        )

        jobs[job.jobId.value] = job
            .copy(executions = job.executions + (execution.thingName to updated))
            .completedIfAllExecutionsTerminal(now)

        return Response(OK).body(
            IotJobsDataPlaneMoshi.asFormatString(
                UpdatedJobExecution(
                    executionState = updated.toState().takeIf { data.includeJobExecutionState == true },
                    jobDocument = job.document.takeIf { data.includeJobDocument == true },
                )
            )
        )
    }

    private fun Request.storedThingName() = thingNameLens(this)

    private fun StoredJobExecution.toSummary(job: StoredJob) = JobExecutionSummary(
        jobId = JobId.of(job.jobId.value),
        queuedAt = queuedAt,
        startedAt = startedAt,
        lastUpdatedAt = lastUpdatedAt,
        versionNumber = versionNumber,
        executionNumber = executionNumber,
    )

    private fun StoredJobExecution.toJobExecution(job: StoredJob, includeJobDocument: Boolean) = JobExecution(
        jobId = JobId.of(job.jobId.value),
        status = JobExecutionStatus.valueOf(status.name),
        versionNumber = versionNumber,
        thingName = ThingName.of(thingName.value),
        statusDetails = statusDetails.takeIf { it.isNotEmpty() },
        queuedAt = queuedAt,
        startedAt = startedAt,
        lastUpdatedAt = lastUpdatedAt,
        approximateSecondsBeforeTimedOut = job.approximateSecondsBeforeTimedOut(this),
        executionNumber = executionNumber,
        jobDocument = job.document.takeIf { includeJobDocument },
    )

    private fun StoredJobExecution.toState() = JobExecutionState(
        status = JobExecutionStatus.valueOf(status.name),
        versionNumber = versionNumber,
        statusDetails = statusDetails.takeIf { it.isNotEmpty() },
    )

    /**
     * Convenience function to get an IotJobsDataPlane client
     */
    fun client() = IotJobsDataPlane.Http(region, { AwsCredentials("accessKey", "secret") }, this)

    companion object {
        private const val NEXT = "\$next"

        private val DEVICE_SETTABLE = setOf(
            JobExecutionStatus.IN_PROGRESS,
            JobExecutionStatus.SUCCEEDED,
            JobExecutionStatus.FAILED,
            JobExecutionStatus.REJECTED,
        )
    }
}

fun main() {
    FakeIotJobsDataPlane().start()
}
