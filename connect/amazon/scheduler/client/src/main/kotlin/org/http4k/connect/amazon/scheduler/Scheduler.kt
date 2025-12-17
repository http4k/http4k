package org.http4k.connect.amazon.scheduler

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/scheduler/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface Scheduler {
    operator fun <R : Any> invoke(action: SchedulerAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("scheduler")
}
