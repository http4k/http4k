package org.http4k.connect.amazon.scheduler

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.scheduler.model.Schedule
import org.http4k.connect.amazon.scheduler.model.ScheduleGroup
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock

class FakeScheduler(
    scheduleGroups: Storage<ScheduleGroup> = Storage.InMemory(),
    schedules: Storage<Schedule> = Storage.InMemory(),
    clock: Clock = Clock.systemUTC(),
    region: Region = Region.US_EAST_1,
    account: AwsAccount = AwsAccount.of("1")
) : ChaoticHttpHandler() {

    private val api =
        AwsRestJsonFake(SchedulerMoshi, AwsService.of("schedule"), region, account)

    override val app = routes(
        api.createScheduleGroup(clock, scheduleGroups),
        api.getScheduleGroup(scheduleGroups),
        api.deleteScheduleGroup(scheduleGroups),
        api.listScheduleGroups(scheduleGroups),
        api.createSchedule(clock, schedules),
        api.getSchedule(schedules),
        api.listSchedules(schedules),
        api.deleteSchedule(schedules),
    )

    /**
     * Convenience function to get a EventBridge client
     */
    fun client() = Scheduler.Http(api.region, { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeScheduler().start()
}
