package org.http4k.connect.amazon.scheduler

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.peek
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.RestfulError
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.scheduler.action.CreateScheduleData
import org.http4k.connect.amazon.scheduler.action.CreateScheduleGroupData
import org.http4k.connect.amazon.scheduler.action.CreatedSchedule
import org.http4k.connect.amazon.scheduler.action.CreatedScheduleGroup
import org.http4k.connect.amazon.scheduler.action.ScheduleGroups
import org.http4k.connect.amazon.scheduler.action.Schedules
import org.http4k.connect.amazon.scheduler.model.Schedule
import org.http4k.connect.amazon.scheduler.model.ScheduleGroup
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupState
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.connect.amazon.scheduler.model.ScheduleState
import org.http4k.connect.amazon.scheduler.model.ScheduleSummary
import org.http4k.connect.amazon.scheduler.model.TargetSummary
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import java.time.Clock


private fun AwsRestJsonFake.scheduleGroupNotFound(name: ScheduleGroupName) =
    RestfulError(NOT_FOUND, "Schedule group does not exist with name '${name}'", null, null)

private fun AwsRestJsonFake.scheduleNotFound(name: ScheduleName) =
    RestfulError(NOT_FOUND, "Schedule does not exist with name '${name}'", null, null)

private val scheduleGroupLens = Path.value(ScheduleGroupName).of("schedule-group")
private val scheduleLens = Path.value(ScheduleName).of("schedule")

fun AwsRestJsonFake.createScheduleGroup(
    clock: Clock,
    scheduleGroups: Storage<ScheduleGroup>
) = "/schedule-groups/$scheduleGroupLens" bind POST to route<CreateScheduleGroupData> { data ->
    val scheduleGroupName = scheduleGroupLens(this)
    val scheduleGroup = ScheduleGroup(
        arn = scheduleGroupName.toArn(),
        creationDate = Timestamp.of(clock.instant()),
        name = scheduleGroupName,
        lastModifiedDate = null,
        state = ScheduleGroupState.ACTIVE
    )

    scheduleGroups[scheduleGroup.name.value] = scheduleGroup

    Success(CreatedScheduleGroup(scheduleGroup.arn))
}


fun AwsRestJsonFake.deleteScheduleGroup(
    scheduleGroups: Storage<ScheduleGroup>
) = "/schedule-groups/$scheduleGroupLens" bind DELETE to route<Unit> {
    val scheduleGroupName = scheduleGroupLens(this)

    scheduleGroups[scheduleGroupName.value]
        .asResultOr { scheduleGroupNotFound(scheduleGroupName) }
        .peek { scheduleGroup -> scheduleGroups.remove(scheduleGroup.name.value) }
        .map { }
}


fun AwsRestJsonFake.getScheduleGroup(
    scheduleGroups: Storage<ScheduleGroup>
) = "/schedule-groups/$scheduleGroupLens" bind GET to route<Unit> {
    val scheduleGroupName = scheduleGroupLens(this)

    scheduleGroups[scheduleGroupName.value]
        .asResultOr { scheduleGroupNotFound(scheduleGroupName) }
}

fun AwsRestJsonFake.listScheduleGroups(
    scheduleGroups: Storage<ScheduleGroup>
) = "/schedule-groups" bind GET to route<Unit> {

    Success(
        ScheduleGroups(
            null,
            scheduleGroups.keySet()
                .mapNotNull { scheduleGroups[it] })
    )
}


fun AwsRestJsonFake.createSchedule(
    clock: Clock,
    schedules: Storage<Schedule>
) = "/schedules/$scheduleLens" bind POST to route<CreateScheduleData> { data ->
    val scheduleName = scheduleLens(this)
    val schedule = Schedule(
        arn = scheduleName.toArn(),
        creationDate = Timestamp.of(clock.instant()),
        name = scheduleName,
        description = data.description,
        actionAfterCompletion = data.actionAfterCompletion,
        lastModifiedDate = null,
        scheduleExpression = data.scheduleExpression,
        scheduleExpressionTimezone = data.scheduleExpressionTimezone,
        flexibleTimeWindow = data.flexibleTimeWindow,
        state = ScheduleState.ENABLED,
        target = data.target,
        startDate = data.startDate,
        endDate = data.endDate,
        kmsKeyArn = data.kmsKeyArn
    )

    schedules[schedule.name.value] = schedule

    Success(CreatedSchedule(schedule.arn))
}

fun AwsRestJsonFake.getSchedule(
    schedules: Storage<Schedule>
) = "/schedules/$scheduleLens" bind GET to route<Unit> {
    val scheduleName = scheduleLens(this)

    schedules[scheduleName.value]
        .asResultOr { scheduleNotFound(scheduleName) }
}

fun AwsRestJsonFake.listSchedules(
    schedules: Storage<Schedule>
) = "/schedules" bind GET to route<Unit> {

    Success(
        Schedules(
            null,
            schedules.keySet()
                .mapNotNull { schedules[it] }
                .map {
                    ScheduleSummary(
                        it.arn,
                        it.name,
                        creationDate = it.creationDate,
                        lastModifiedDate = it.lastModifiedDate,
                        target = it.target?.let { t -> TargetSummary(t.arn) },
                        state = it.state,
                    )
                }
        )
    )
}

fun AwsRestJsonFake.deleteSchedule(
    schedules: Storage<Schedule>
) = "/schedules/$scheduleLens" bind DELETE to route<Unit> {
    val scheduleName = scheduleLens(this)

    schedules[scheduleName.value]
        .asResultOr { scheduleNotFound(scheduleName) }
        .peek { scheduleGroup -> schedules.remove(scheduleGroup.name.value) }
        .map { }
}

private fun ScheduleGroupName.toArn() = ARN.of(
    Scheduler.awsService,
    Region.of("us-east-1"),
    AwsAccount.of("0"),
    "schedule-group", this
)


private fun ScheduleName.toArn() = ARN.of(
    Scheduler.awsService,
    Region.of("us-east-1"),
    AwsAccount.of("0"),
    "schedule", this
)
