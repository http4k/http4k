package org.http4k.lens


import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.cloudevents.CloudEvent
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withContentType
import io.cloudevents.jackson.JsonCloudEventData
import io.cloudevents.jackson.PojoCloudEventDataMapper
import io.cloudevents.rw.CloudEventDataMapper
import org.http4k.cloudevents.toCloudEventReader
import org.http4k.cloudevents.write
import org.http4k.core.Body
import org.http4k.core.CLOUD_EVENT_JSON
import org.http4k.core.ContentType
import org.http4k.format.ConfigurableJackson
import org.http4k.lens.ParamMeta.ObjectParam


fun Body.Companion.cloudEvent(contentType: ContentType = ContentType.CLOUD_EVENT_JSON) = BiDiBodyLensSpec<CloudEvent>(
    listOf(Meta(true, "body", ObjectParam, "Cloud Event", "Cloud Event")),
    contentType,
    LensGet { _, target -> listOf(target.toCloudEventReader().toEvent()) },
    LensSet { _, values, target ->
        values.fold(target) { memo, next ->
            memo.write(next)
        }
    }
)

inline fun <reified T : CloudEventData> ConfigurableJackson.cloudEventDataLens(): BiDiLens<CloudEvent, T> {
    val get = LensGet<CloudEvent, T> { _, target ->
        target.data?.let {
            listOf(
                PojoCloudEventDataMapper.from(mapper, jacksonTypeRef<T>()).map(it).value
            )
        } ?: emptyList()
    }

    val set = LensSet<CloudEvent, T> { _, values, event ->
        values.fold(event) { acc, next ->
            CloudEventBuilder.from(acc)
                .withContentType(ContentType.CLOUD_EVENT_JSON)
                .withData(JsonCloudEventData.wrap(asJsonObject(next)))
                .build()
        }
    }

    return object : BiDiLensSpec<CloudEvent, T>("Cloud Event", ObjectParam, get, set) {}.required("data")
}

object CloudEvent {
    fun <T : CloudEventData> data(mapper: CloudEventDataMapper<T>): Lens<CloudEvent, T> =
        object : LensSpec<CloudEvent, T>(
            "Cloud Event",
            ObjectParam,
            LensGet { _, target -> target.data?.let { listOf(mapper.map(it)) } ?: emptyList() }
        ) {}.required("data")
}
