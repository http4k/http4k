package org.http4k.connect.amazon.scheduler.model

import com.squareup.moshi.Json
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Schedule(
    @Json(name = "Arn") val arn: ARN,
    @Json(name = "Name") val name: ScheduleName,
    @Json(name = "CreationDate") val creationDate: Timestamp,
    @Json(name = "LastModifiedDate") val lastModifiedDate: Timestamp?,
    @Json(name = "ScheduleExpression") val scheduleExpression: ScheduleExpression,
    @Json(name = "ScheduleExpressionTimezone") val scheduleExpressionTimezone: String?,
    @Json(name = "FlexibleTimeWindow") val flexibleTimeWindow: FlexibleTimeWindow,
    @Json(name = "Target") val target: Target?,
    @Json(name = "State") val state: ScheduleState? = null,
    @Json(name = "Description") val description: String?,
    @Json(name = "ActionAfterCompletion") val actionAfterCompletion: ScheduleActionAfterCompletion?,
    @Json(name = "StartDate") val startDate: Timestamp?,
    @Json(name = "EndDate") val endDate: Timestamp?,
    @Json(name = "KmsKeyArn") val kmsKeyArn: ARN?,
)

