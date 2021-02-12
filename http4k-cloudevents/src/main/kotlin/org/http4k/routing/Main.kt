package org.http4k.routing

import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder.v1
import io.cloudevents.core.builder.withContentType
import io.cloudevents.core.builder.withSourceUri
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.jackson.JsonFormat
import org.http4k.core.Body
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.POST
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.debug
import org.http4k.lens.CloudEvent
import org.http4k.lens.cloudEvent

fun main() {
    val app = Filter.NoOp
        .then(routes(
            "/foo/bar" bind POST to {
                val eventLens = Body.cloudEvent().toLens()
                val eventdata = CloudEvent.data<MyCloudEventData>()(eventLens(it))
                println(eventdata)
                Response(OK)
            }
        )).debug()

    val lens = Body.cloudEvent().toLens()

    EventFormatProvider.getInstance().registerFormat(JsonFormat())

    val data = MyCloudEventData(10)

    val cloudEvent = v1()
        .withId("aaa")
        .withSourceUri(Uri.of("localhost"))
        .withContentType(ContentType.CLOUD_EVENT_JSON)
        .withType("bbb")
        .withData(data)
        .build()

    println(
        app(
            Request(POST, "/foo/bar").with(
                lens of cloudEvent
            )
        )
    )
}

data class MyCloudEventData(val value: Int) : CloudEventData {
    override fun toBytes() = value.toString().toByteArray()
}
