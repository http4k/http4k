package org

import io.cloudevents.core.builder.CloudEventBuilder.v03
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.jackson.JsonFormat
import io.cloudevents.with
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.filter.debug
import org.http4k.format.Jackson
import org.http4k.format.MyCloudEventData
import org.http4k.format.cloudEventDataLens
import org.http4k.lens.cloudEvent
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {

    EventFormatProvider.getInstance().registerFormat(JsonFormat())

    val eventLens = Body.cloudEvent().toLens()
    val dataLens = Jackson.cloudEventDataLens<MyCloudEventData>()

    val app = ServerFilters.CatchLensFailure()
        .then(routes(
            "/foo/bar" bind POST to {
                val cloudEvent = eventLens(it)
                val eventData = dataLens(cloudEvent)
                println(eventData)
                Response(OK)
            }
        )).debug()

    val data = MyCloudEventData(10)

    val cloudEvent = v03()
        .withId("aaa")
        .withSource(Uri.of("localhost"))
        .withType("bbb")
        .build()

    println(
        app(
            Request(POST, "/foo/bar").with(eventLens of cloudEvent.with(dataLens of data))
        )
    )
}
