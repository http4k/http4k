package org.http4k.format

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import kotlin.reflect.KClass

abstract class AutoMarshallingJson : AutoMarshalling() {
    @Deprecated("Use asFormatString instead", ReplaceWith("asFormatString(input"))
    fun asJsonString(input: Any): String = asFormatString(input)
}

abstract class JsonLibAutoMarshallingJson<NODE : Any> : AutoMarshallingJson(), Json<NODE> {
    override fun asFormatString(input: Any): String = compact(asJsonObject(input))

    abstract fun asJsonObject(input: Any): NODE

    abstract fun <T : Any> asA(j: NODE, target: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): NODE = asJsonObject(this)

    @JvmName("nodeAsA")
    fun <T : Any> NODE.asA(target: KClass<T>): T = asA(this, target)
}

fun httpBodyLens(description: String? = null, contentNegotiation: ContentNegotiation = None, contentType: ContentType) = httpBodyRoot(listOf(Meta(true, "body", ObjectParam, "body", description)), contentType, contentNegotiation)
    .map({ it.payload.asString() }, { Body(it) })
