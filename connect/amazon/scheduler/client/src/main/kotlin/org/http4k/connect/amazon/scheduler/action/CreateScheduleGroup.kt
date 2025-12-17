package org.http4k.connect.amazon.scheduler.action

import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class CreateScheduleGroup(
    val name: ScheduleGroupName,
    val clientToken: ClientToken,
    val tags: List<Tag>? = null
) : SchedulerAction<CreatedScheduleGroup>(CreatedScheduleGroup::class),
    Action<Result<CreatedScheduleGroup, RemoteFailure>> {

    override fun uri() = Uri.of("/schedule-groups/${name.value}")

    override fun requestBody() = CreateScheduleGroupData(
        clientToken,
        tags
    )
}

@JsonSerializable
data class CreateScheduleGroupData(
    @Json(name = "ClientToken") val clientToken: ClientToken,
    @Json(name = "Tags") val tags: List<Tag>?
)

@JsonSerializable
data class CreatedScheduleGroup(
    @Json(name = "ScheduleGroupArn") val scheduleGroupArn: ARN
)



