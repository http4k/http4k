package org

import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withSourceUri
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.jackson.JsonFormat
import org.http4k.cloudevents.with
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.filter.debug
import org.http4k.format.Jackson
import org.http4k.lens.cloudEvent
import org.http4k.lens.cloudEventDataLens
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {

    EventFormatProvider.getInstance().registerFormat(JsonFormat())

    val eventLens = Body.cloudEvent().toLens()
    val dataLens = Jackson.cloudEventDataLens<MyCloudEventData>()

    val app = ServerFilters.CatchLensFailure { Response(BAD_REQUEST).body(it.cause!!.message!!)}
        .then(routes(
            "/foo/bar" bind POST to {
                val cloudEvent = eventLens(it)
                val eventData = dataLens(cloudEvent)
                Response(OK)
            }
        )).debug()


    val data = MyCloudEventData(10)

    val cloudEvent = CloudEventBuilder.v03()
        .withId("aaa")
        .withSourceUri(Uri.of("localhost"))
        .withType("bbb")
        .build()

    println(
        app(
            Request(POST, "/foo/bar").with(eventLens of cloudEvent.with(dataLens of data))
        )
    )
}


data class MyCloudEventData(val value: Int) : CloudEventData {
    override fun toBytes() = value.toString().toByteArray()
}
