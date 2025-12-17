package org.http4k.connect.amazon.scheduler.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query

@Http4kConnectAction
data class DeleteSchedule(
    val name: ScheduleName,
    val groupName: ScheduleGroupName? = null,
    val clientToken: ClientToken
) : SchedulerAction<Unit>(Unit::class, method = Method.DELETE),
    Action<Result<Unit, RemoteFailure>> {

    override fun uri() = Uri.of("/schedules/${name.value}")
        .query("clientToken", clientToken.value)
        .let { rq -> groupName?.let { rq.query("groupName", groupName.value) } ?: rq }

    override fun requestBody() = Unit

}
