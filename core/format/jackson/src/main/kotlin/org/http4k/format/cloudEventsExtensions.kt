package org.http4k.format

import tools.jackson.module.kotlin.jacksonTypeRef
import io.cloudevents.CloudEvent
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.http4k.lens.Lens
import org.http4k.lens.LensGet
import org.http4k.lens.LensSpec
import org.http4k.lens.ParamMeta.ObjectParam
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.MapperBuilder
import kotlin.text.orEmpty

inline fun <reified T : Any> ConfigurableJackson.cloudEventDataLens(): Lens<CloudEvent, T> {
//    mapper.rebuild<*>().deactivateDefaultTyping()
    val get = LensGet<CloudEvent, T> { _, target ->
        target.data?.let { listOf(PojoCloudEventDataMapper.from(mapper, jacksonTypeRef<T>()).map(it).value) }
            .orEmpty()
    }

    return object : LensSpec<CloudEvent, T>("CloudEvent", ObjectParam, get) {}.required(T::class.simpleName!!)
}
