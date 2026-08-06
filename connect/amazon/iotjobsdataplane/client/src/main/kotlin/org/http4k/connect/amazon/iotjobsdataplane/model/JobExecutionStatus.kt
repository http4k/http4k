package org.http4k.connect.amazon.iotjobsdataplane.model

/**
 * Of these, a device may only set IN_PROGRESS, SUCCEEDED, FAILED and REJECTED through
 * UpdateJobExecution.
 */
enum class JobExecutionStatus {
    QUEUED, IN_PROGRESS, SUCCEEDED, FAILED, TIMED_OUT, REJECTED, REMOVED, CANCELED
}
