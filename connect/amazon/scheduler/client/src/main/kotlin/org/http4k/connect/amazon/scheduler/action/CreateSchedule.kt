package org.http4k.connect.amazon.scheduler.action

import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.FlexibleTimeWindow
import org.http4k.connect.amazon.scheduler.model.ScheduleActionAfterCompletion
import org.http4k.connect.amazon.scheduler.model.ScheduleExpression
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.connect.amazon.scheduler.model.Target
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class CreateSchedule(
    val name: ScheduleName,
    val clientToken: ClientToken,
    val scheduleExpression: ScheduleExpression,
    val scheduleExpressionTimezone: String?,
    val flexibleTimeWindow: FlexibleTimeWindow,
    val target: Target,
    val groupName: ScheduleGroupName? = null,
    val description: String? = null,
    val actionAfterCompletion: ScheduleActionAfterCompletion? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val kmsKeyArn: ARN?
) : SchedulerAction<CreatedSchedule>(CreatedSchedule::class),
    Action<Result<CreatedSchedule, RemoteFailure>> {

    override fun uri() = Uri.of("/schedules/${name.value}")

    override fun requestBody() = CreateScheduleData(
        clientToken,
        scheduleExpression,
        scheduleExpressionTimezone,
        flexibleTimeWindow,
        target,
        groupName,
        description,
        actionAfterCompletion,
        startDate,
        endDate,
        kmsKeyArn
    )
}

@JsonSerializable
data class CreateScheduleData(
    @Json(name = "ClientToken") val clientToken: ClientToken,
    @Json(name = "ScheduleExpression") val scheduleExpression: ScheduleExpression,
    @Json(name = "ScheduleExpressionTimezone") val scheduleExpressionTimezone: String?,
    @Json(name = "FlexibleTimeWindow") val flexibleTimeWindow: FlexibleTimeWindow,
    @Json(name = "Target") val target: Target,
    @Json(name = "GroupName") val groupName: ScheduleGroupName?,
    @Json(name = "Description") val description: String?,
    @Json(name = "ActionAfterCompletion") val actionAfterCompletion: ScheduleActionAfterCompletion?,
    @Json(name = "StartDate") val startDate: Timestamp?,
    @Json(name = "EndDate") val endDate: Timestamp?,
    @Json(name = "KmsKeyArn") val kmsKeyArn: ARN?,
)

@JsonSerializable
data class CreatedSchedule(
    @Json(name = "ScheduleArn") val scheduleArn: ARN
)



