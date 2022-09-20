package org.http4k.format

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.cloudevents.CloudEvent
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withDataContentType
import io.cloudevents.jackson.JsonCloudEventData
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Lens
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.LensSpec
import org.http4k.lens.ParamMeta.ObjectParam

inline fun <reified T : Any> ConfigurableJackson.cloudEventDataLens(): Lens<CloudEvent, T> {
    val get = LensGet<CloudEvent, T> { _, target ->
        target.data?.let { listOf(PojoCloudEventDataMapper.from(mapper, jacksonTypeRef<T>()).map(it).value) }
            ?: emptyList()
    }

    return object : LensSpec<CloudEvent, T>("CloudEvent", ObjectParam, get) {}.required(T::class.simpleName!!)
}
