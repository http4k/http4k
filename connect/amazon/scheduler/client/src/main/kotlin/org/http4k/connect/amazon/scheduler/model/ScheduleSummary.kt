package org.http4k.connect.amazon.scheduler.model

import com.squareup.moshi.Json
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ScheduleSummary(
    @Json(name = "Arn") val arn: ARN,
    @Json(name = "Name") val name: ScheduleName,
    @Json(name = "CreationDate") val creationDate: Timestamp,
    @Json(name = "LastModifiedDate") val lastModifiedDate: Timestamp?,
    @Json(name = "Target") val target: TargetSummary?,
    @Json(name = "State") val state: ScheduleState? = null
)


@JsonSerializable
data class TargetSummary(@Json(name = "Arn") val arn: ARN)
