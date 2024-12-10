package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cloudwatchlogs.action.FilteredLogEvent
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes

data class LogGroup(val streams: MutableMap<LogStreamName, MutableList<FilteredLogEvent>>)

class FakeCloudWatchLogs(val logGroup: Storage<LogGroup> = Storage.InMemory()) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(CloudWatchLogsMoshi, AwsService.of("Logs_20140328"))

    override val app = routes(
        "/" bind POST to routes(
            api.createLogGroup(logGroup),
            api.createLogStream(logGroup),
            api.deletaLogGroup(logGroup),
            api.deletaLogStream(logGroup),
            api.putLogEvents(logGroup),
            api.filterLogEvents(logGroup)
        )
    )

    /**
     * Convenience function to get a CloudWatchLogs client
     */
    fun client() = CloudWatchLogs.Http(Region.of("ldn-north-1"), { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeCloudWatchLogs().start()
}
