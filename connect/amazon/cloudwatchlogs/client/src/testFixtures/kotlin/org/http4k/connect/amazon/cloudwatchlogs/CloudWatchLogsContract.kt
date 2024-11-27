package org.http4k.connect.amazon.cloudwatchlogs

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.cloudwatchlogs.action.LogEvent
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import org.http4k.connect.model.TimestampMillis
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import java.time.Clock

interface CloudWatchLogsContract : AwsContract {

    private val clock get() = Clock.systemUTC()

    private val cloudWatchLogs
        get() =
        CloudWatchLogs.Http(aws.region, { aws.credentials }, http.debug())

    private val logGroupName get() = LogGroupName.of(uuid().toString())
    private val logStreamName get() = LogStreamName.of(uuid().toString())

    @Test
    fun `log events lifecycle`() {
        with(cloudWatchLogs) {
            createLogGroup(logGroupName, mapOf("1" to "2")).valueOrNull()!!
            createLogStream(logGroupName, logStreamName).valueOrNull()!!
            try {
                putLogEvents(
                    logGroupName, logStreamName, listOf(
                        LogEvent("hello", TimestampMillis.of(clock.instant())),
                        LogEvent("world", TimestampMillis.of(clock.instant()))
                    )
                ).valueOrNull()

                Thread.sleep(2000)

                val eventResults = filterLogEventsPaginated(
                    logGroupName, false,
                    logStreamNames = listOf(logStreamName),
                    limit = 1
                ).take(2).toList()
                val result = eventResults.allValues().valueOrNull()!!.flatten()
                assertThat(result.size, equalTo(2))
            } catch (e: Exception) {
                deleteLogGroup(logGroupName)
                deleteLogStream(logGroupName, logStreamName)
            }
        }
    }
}
