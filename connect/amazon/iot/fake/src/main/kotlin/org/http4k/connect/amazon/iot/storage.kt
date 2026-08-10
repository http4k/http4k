package org.http4k.connect.amazon.iot

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.model.JobExecutionStatus
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.JobStatus
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.TargetSelection
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.amazon.iot.model.TimeoutConfig
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage

/**
 * The job record FakeIot keeps, keyed by jobId. Public so that a Storage can be shared with
 * FakeIotJobsDataPlane: construct both fakes over the same Storage instance and the control
 * plane and the device API see one jobs state.
 *
 * Mutating handlers in both fakes synchronize on the Storage instance itself, so when one
 * store is shared across fakes and threads (a test driving the control plane while a bridge
 * relays device-side updates), each request's read-modify-write is atomic and a cancel or
 * delete cannot be overwritten by a concurrently relayed device update.
 */
data class StoredJob(
    val jobId: JobId,
    val jobArn: ARN,
    val targets: List<ARN>,
    val document: String?,
    val description: String?,
    val targetSelection: TargetSelection,
    val status: JobStatus,
    val comment: String?,
    val forceCanceled: Boolean?,
    val timeoutConfig: TimeoutConfig?,
    val createdAt: Timestamp,
    val lastUpdatedAt: Timestamp,
    val completedAt: Timestamp?,
    val executions: Map<ThingName, StoredJobExecution>,
)

/**
 * The stream record FakeIot keeps, keyed by streamId. A stream's bytes are not modelled: the
 * fake records where they were said to be, since devices fetch them over MQTT rather than from
 * this API.
 */
data class StoredStream(
    val streamId: StreamId,
    val streamArn: ARN,
    val streamVersion: Int,
    val files: List<StreamFile>,
    val roleArn: ARN,
    val description: String?,
    val createdAt: Timestamp,
    val lastUpdatedAt: Timestamp,
)

data class StoredJobExecution(
    val thingName: ThingName,
    val thingArn: ARN,
    val status: JobExecutionStatus,
    val statusDetails: Map<String, String>,
    val queuedAt: Timestamp,
    val startedAt: Timestamp?,
    val lastUpdatedAt: Timestamp,
    val versionNumber: Long,
    val executionNumber: Long,
    /** Set when a force cancel took this execution out of IN_PROGRESS, as AWS reports it. */
    val forceCanceled: Boolean? = null,
)

/**
 * The thing an `arn:...:thing/NAME` target names, or null for any other resource type - a
 * `thinggroup/NAME` target most of all, whose tail is a group name and not a device.
 */
fun ARN.thingNameOrNull() = value.substringAfterLast(":")
    .takeIf { it.startsWith("thing/") }
    ?.removePrefix("thing/")
    ?.let(ThingName::of)

val JobExecutionStatus.isTerminal
    get() = this != JobExecutionStatus.QUEUED && this != JobExecutionStatus.IN_PROGRESS

/** Every (job, execution) pair for the thing, oldest first. */
fun Storage<StoredJob>.executionsFor(thingName: ThingName) = keySet()
    .mapNotNull { this[it] }
    .mapNotNull { job -> job.executions[thingName]?.let { job to it } }
    .sortedWith(compareBy({ it.second.queuedAt.value }, { it.first.jobId.value }))

/**
 * The `$next` selection: the oldest IN_PROGRESS execution for the thing first, else the
 * oldest QUEUED one, considering only jobs which are themselves still IN_PROGRESS. This is
 * a read: callers decide whether the pick transitions.
 */
fun Storage<StoredJob>.nextPendingExecutionFor(thingName: ThingName): Pair<StoredJob, StoredJobExecution>? {
    val candidates = executionsFor(thingName).filter { (job, _) -> job.status == JobStatus.IN_PROGRESS }

    return candidates.firstOrNull { (_, execution) -> execution.status == JobExecutionStatus.IN_PROGRESS }
        ?: candidates.firstOrNull { (_, execution) -> execution.status == JobExecutionStatus.QUEUED }
}

/**
 * When every execution of an in-progress SNAPSHOT job is terminal, the job completes.
 *
 * A CONTINUOUS job never does: its target set is open, so AWS keeps it in progress to enrol
 * things added later, and a caller waiting for COMPLETED on one would wait forever.
 */
fun StoredJob.completedIfAllExecutionsTerminal(now: Timestamp) = when {
    status == JobStatus.IN_PROGRESS &&
        targetSelection == TargetSelection.SNAPSHOT &&
        executions.values.all { it.status.isTerminal } ->
        copy(status = JobStatus.COMPLETED, completedAt = now, lastUpdatedAt = now)

    else -> this
}

/**
 * The fake runs no live timers, so an in-progress execution of a job with a timeout config
 * always reports its full budget as remaining.
 */
fun StoredJob.approximateSecondsBeforeTimedOut(execution: StoredJobExecution) =
    timeoutConfig?.inProgressTimeoutInMinutes
        ?.takeIf { execution.status == JobExecutionStatus.IN_PROGRESS }
        ?.let { it * 60 }
