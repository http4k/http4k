package org.http4k.connect.amazon.iot.model

import se.ansman.kotshi.JsonSerializable

/**
 * The amount of time each device has to finish its execution of the job, in minutes
 * (1 to 10080). The timer starts when the execution status is set to IN_PROGRESS.
 */
@JsonSerializable
data class TimeoutConfig(
    val inProgressTimeoutInMinutes: Long? = null
)
