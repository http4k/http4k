package org.http4k.connect.amazon.iotjobsdataplane.model

import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

/**
 * A job execution as the jobs data plane reports it. Unlike the control plane, the status
 * details are a bare map and the job document travels inline. All timestamps are seconds
 * since the epoch.
 */
@JsonSerializable
data class JobExecution(
    val jobId: JobId,
    val status: JobExecutionStatus,
    val versionNumber: Long,
    val thingName: ThingName? = null,
    val statusDetails: Map<String, String>? = null,
    val queuedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val approximateSecondsBeforeTimedOut: Long? = null,
    val executionNumber: Long? = null,
    val jobDocument: String? = null,
)
