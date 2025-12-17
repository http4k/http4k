package org.http4k.connect.amazon.scheduler.action

import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ScheduleGroup
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ListScheduleGroups(
    val maxResults: Int? = null,
    val namePrefix: ScheduleGroupName? = null,
    val nextToken: String? = null
) : SchedulerAction<ScheduleGroups>(ScheduleGroups::class, method = Method.GET),
    Action<Result<ScheduleGroups, RemoteFailure>> {

    override fun uri() = Uri.of("/schedule-groups")
        .let { rq -> maxResults?.let { rq.query("MaxResults", it.toString()) } ?: rq }
        .let { rq -> namePrefix?.let { rq.query("NamePrefix", it.value) } ?: rq }
        .let { rq -> nextToken?.let { rq.query("NextToken", it) } ?: rq }
    override fun requestBody() = Unit

}


@JsonSerializable
data class ScheduleGroups(
    @Json(name = "NextToken") val nextToken: String?,
    @Json(name = "ScheduleGroups") val scheduleGroups: List<ScheduleGroup>
)
