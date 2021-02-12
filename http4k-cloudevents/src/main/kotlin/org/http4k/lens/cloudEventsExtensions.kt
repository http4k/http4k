package org.http4k.lens


import io.cloudevents.CloudEvent
import io.cloudevents.CloudEventData
import org.http4k.cloudevents.toCloudEventReader
import org.http4k.cloudevents.write
import org.http4k.core.Body
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.lens.ParamMeta.ObjectParam

fun Body.Companion.cloudEvent(contentType: ContentType = ContentType.CLOUD_EVENT_JSON) = BiDiBodyLensSpec<CloudEvent>(
    listOf(Meta(true, "body", ObjectParam, "Cloud Event", "Cloud Event")),
    contentType,
    LensGet { _, target -> listOf(target.toCloudEventReader().toEvent()) },
    LensSet { _, values, target -> values.fold(target) { memo, next -> memo.write(next) } }
)

object CloudEvent {
    fun <T : CloudEventData> data(): Lens<CloudEvent, T> = object : LensSpec<CloudEvent, T>(
        "Cloud Event",
        ObjectParam,
        LensGet { _, target -> listOf() }
    ) {}.required("data")
}
