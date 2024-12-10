package org.http4k.connect.amazon.eventbridge

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.eventbridge.model.Event
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeEventBridge(val events: Storage<List<Event>> = Storage.InMemory()) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(EventBridgeMoshi, AwsService.of("AWSEvents"))

    override val app = routes(
        "/" bind POST to routes(
            api.createEventBus(events),
            api.listEventBuses(events),
            api.deleteEventBus(events),
            api.putEvents(events),
        )
    )

    /**
     * Convenience function to get a EventBridge client
     */
    fun client() = EventBridge.Http(Region.of("ldn-north-1"), { AwsCredentials("accessKey", "secret") }, this)
}

fun main() {
    FakeEventBridge().start()
}
