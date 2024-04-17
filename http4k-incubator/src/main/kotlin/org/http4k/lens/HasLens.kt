package org.http4k.lens

import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.lens.ParamMeta.ObjectParam
import kotlin.reflect.KClass

/**
 * Experimental template for a companion object to provide a lens into a class
 */
abstract class HasLens<T : Any>(
    private val autoMarshalling: AutoMarshalling,
    private val clazz: KClass<T>,
    metadata: Map<String, Any> = emptyMap()
) {
    val lens = BiDiBodyLens(
        listOf(Meta(true, "body", ObjectParam, clazz.java.simpleName, null, metadata)),
        autoMarshalling.defaultContentType,
        { autoMarshalling.asA(it.bodyString(), clazz) },
        { value, target ->
            target.body(autoMarshalling.asFormatString(value))
                .with(Header.CONTENT_TYPE of autoMarshalling.defaultContentType)
        })

    companion object {
        @JvmStatic
        protected inline fun <reified T : Any> kClass(): KClass<T> = T::class
    }
}
