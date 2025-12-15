package org.http4k.connect.amazon.scheduler.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ScheduleGroup
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.core.Method
import org.http4k.core.Uri

@Http4kConnectAction
data class GetScheduleGroup(
    val groupName: ScheduleGroupName
) : SchedulerAction<ScheduleGroup>(ScheduleGroup::class, method = Method.GET),
    Action<Result<ScheduleGroup, RemoteFailure>> {

    override fun uri() = Uri.of("/schedule-groups/${groupName.value}")
    override fun requestBody() = Unit

}


