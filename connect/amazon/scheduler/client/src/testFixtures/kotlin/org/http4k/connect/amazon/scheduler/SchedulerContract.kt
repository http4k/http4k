package org.http4k.connect.amazon.scheduler

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.FlexibleTimeWindow
import org.http4k.connect.amazon.scheduler.model.ScheduleExpression
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.connect.amazon.scheduler.model.Target
import org.http4k.connect.amazon.scheduler.model.TimeWindowMode
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

interface SchedulerContract : AwsContract {
    private val scheduler get() = Scheduler.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `schedule group lifecycle`() {
        val scheduleGroupName = ScheduleGroupName.of(uuid().toString().take(8))

        with(scheduler) {
            try {
                val scheduleGroupArn = createScheduleGroup(
                    scheduleGroupName,
                    ClientToken.random()
                ).successValue().scheduleGroupArn

                assertThat(
                    listScheduleGroups().successValue().scheduleGroups.map { it.name.value }
                        .contains(scheduleGroupName.value),
                    equalTo(true)
                )

                assertThat(
                    getScheduleGroup(
                        groupName = scheduleGroupName
                    ).successValue().name,
                    equalTo(scheduleGroupName)
                )

                val scheduleName = ScheduleName.of(uuid().toString().take(8))
                createSchedule(
                    name = scheduleName,
                    clientToken = ClientToken.random(),
                    scheduleExpression = ScheduleExpression.of("rate(1 hour)"),
                    flexibleTimeWindow = FlexibleTimeWindow(TimeWindowMode.OFF),
                    target = Target(
                        ARN.of("arn:aws:sns:eu-west-1:${scheduleGroupArn.account}:TOPICTEST"),
                        ARN.of("arn:aws:iam::${scheduleGroupArn.account}:role/ROLETEST"),
                        "test"
                    ),
                    groupName = scheduleGroupName

                ).successValue().scheduleArn

                assertThat(
                    getSchedule(
                        name = scheduleName,
                        groupName = scheduleGroupName
                    ).successValue().name,
                    equalTo(scheduleName)
                )

                assertThat(
                    listSchedules(groupName = scheduleGroupName).successValue().schedules.map { it.name.value }
                        .contains(scheduleName.value),
                    equalTo(true)
                )

                deleteSchedule(scheduleName, scheduleGroupName, ClientToken.random())
                    .successValue()

                assertThat(
                    listSchedules(groupName = scheduleGroupName).successValue().schedules.map { it.name.value }
                        .contains(scheduleName.value),
                    equalTo(false)
                )

            } finally {
                deleteScheduleGroup(scheduleGroupName, ClientToken.random()).successValue()
            }
        }
    }
}
