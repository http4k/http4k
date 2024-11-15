package org.http4k.connect.amazon.eventbridge

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.eventbridge.model.Event
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.EventDetail
import org.http4k.connect.amazon.model.EventDetailType
import org.http4k.connect.amazon.model.EventSource
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.Test
import java.util.UUID

interface EventBridgeContract : AwsContract {
    private val eventBridge get() = EventBridge.Http(aws.region, { aws.credentials }, http)

    private val eventBusName get() = EventBusName.of(uuid().toString())

    @Test
    fun `delivery stream lifecycle`() {
        with(eventBridge) {
            try {
                val arn = createEventBus(eventBusName).successValue().EventBusArn

                assertThat(
                    listEventBuses().successValue().EventBuses.map { it.Name }.contains(eventBusName),
                    equalTo(true)
                )
                assertThat(
                    putEvents(
                        listOf(
                            Event(
                                EventDetail.of("{}"),
                                EventDetailType.of("detail type"),
                                EventSource.of("foobar"),
                                eventBusName
                            ),
                            Event(
                                EventDetail.of("{}"),
                                EventDetailType.of("detail type"),
                                EventSource.of("foobar"),
                                arn
                            ),
                            Event(
                                EventDetail.of("{}"),
                                EventDetailType.of("detail type"),
                                EventSource.of("foobar"),
                                eventBusName
                            )
                        )
                    ).successValue().Entries.all { it.EventId != null },
                    equalTo(true)
                )
                assertThat(
                    putEvents(
                        listOf(
                            Event(
                                EventDetail.of("{}"),
                                EventDetailType.of("detail type"),
                                EventSource.of("foobar"),
                                eventBusName
                            )
                        )
                    ).successValue().Entries.all { it.EventId != null },
                    equalTo(true)
                )
            } finally {
                deleteEventBus(eventBusName).successValue()
            }
        }
    }
}
