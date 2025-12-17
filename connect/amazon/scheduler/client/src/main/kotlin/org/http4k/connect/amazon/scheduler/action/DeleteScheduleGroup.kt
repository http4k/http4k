package org.http4k.connect.amazon.scheduler.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.scheduler.SchedulerAction
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.query

@Http4kConnectAction
data class DeleteScheduleGroup(
    val name: ScheduleGroupName,
    val clientToken: ClientToken
) : SchedulerAction<Unit>(Unit::class, method = Method.DELETE),
    Action<Result<Unit, RemoteFailure>> {

    override fun uri() = Uri.of("/schedule-groups/${name.value}").query("clientToken", clientToken.value)

    override fun requestBody() = Unit

}
