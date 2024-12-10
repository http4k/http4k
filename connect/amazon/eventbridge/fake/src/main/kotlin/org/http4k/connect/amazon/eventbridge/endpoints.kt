package org.http4k.connect.amazon.eventbridge

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.eventbridge.action.CreateEventBus
import org.http4k.connect.amazon.eventbridge.action.CreatedEventBus
import org.http4k.connect.amazon.eventbridge.action.DeleteEventBus
import org.http4k.connect.amazon.eventbridge.action.EventBuses
import org.http4k.connect.amazon.eventbridge.action.EventResult
import org.http4k.connect.amazon.eventbridge.action.EventResults
import org.http4k.connect.amazon.eventbridge.action.ListEventBuses
import org.http4k.connect.amazon.eventbridge.action.PutEvents
import org.http4k.connect.amazon.eventbridge.model.Event
import org.http4k.connect.amazon.eventbridge.model.EventBus
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.EventId
import org.http4k.connect.storage.Storage
import java.util.UUID

fun AwsJsonFake.putEvents(events: Storage<List<Event>>) = route<PutEvents> {
    val newEvents = it.Entries.groupBy {
        it.EventBusName
            ?.let { if (it.value.startsWith("arn")) ARN.of(it.value).resourceId(EventBusName::of) else it }
            ?: EventBusName.of("default")
    }

    EventResults(newEvents.flatMap { (bus, new) ->
        val before = events[bus.value] ?: listOf()
        events[bus.value] = before + new
        List(new.size) {
            EventResult(
                EventId.of(UUID(0, (before.size + it).toLong()).toString()),
                null, null
            )
        }
    }, 0)
}

fun AwsJsonFake.createEventBus(events: Storage<List<Event>>) = route<CreateEventBus> {
    events[it.Name.value] = listOf()
    CreatedEventBus(it.Name.toArn())
}

fun AwsJsonFake.listEventBuses(events: Storage<List<Event>>) = route<ListEventBuses> {
    EventBuses(events.keySet().map {
        EventBus(
            EventBusName.of(it).toArn(),
            EventBusName.of(it),
            null
        )
    }, null)
}

fun AwsJsonFake.deleteEventBus(records: Storage<List<Event>>) = route<DeleteEventBus> {
    records.remove(it.Name.value)
    Unit
}

private fun EventBusName.toArn() = ARN.of(
    EventBridge.awsService,
    Region.of("us-east-1"),
    AwsAccount.of("0"),
    "event-bus", this
)
