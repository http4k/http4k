package org.http4k.connect.amazon.scheduler.action

import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ScheduleSummary
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.connect.amazon.scheduler.model.ScheduleState
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ListSchedules(
    val maxResults: Int? = null,
    val namePrefix: ScheduleName? = null,
    val groupName: ScheduleGroupName? = null,
    val state: ScheduleState? = null,
    val nextToken: String? = null
) : SchedulerAction<Schedules>(Schedules::class, method = Method.GET),
    Action<Result<Schedules, RemoteFailure>> {

    override fun uri() = Uri.of("/schedules")
        .let { rq -> maxResults?.let { rq.query("MaxResults", maxResults.toString()) } ?: rq }
        .let { rq -> namePrefix?.let { rq.query("NamePrefix", namePrefix.value) } ?: rq }
        .let { rq -> groupName?.let { rq.query("GroupName", groupName.value) } ?: rq }
        .let { rq -> state?.let { rq.query("State", state.name) } ?: rq }
        .let { rq -> nextToken?.let { rq.query("NextToken", nextToken) } ?: rq }

    override fun requestBody() = Unit

}


@JsonSerializable
data class Schedules(
    @Json(name = "NextToken") val nextToken: String?,
    @Json(name = "Schedules") val schedules: List<ScheduleSummary>
)
