package org.http4k.format

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import java.io.InputStream
import kotlin.reflect.KClass

abstract class AutoMarshalling {
    abstract fun <T : Any> asA(input: String, target: KClass<T>): T

    @JvmName("stringAsA")
    inline fun <reified T : Any> asA(input: String): T = asA(input, T::class)

    @JvmName("stringAsA")
    fun <T : Any> String.asA(target: KClass<T>): T = asA(this, target)

    abstract fun asString(input: Any): String

    fun asInputStream(input: Any): InputStream = asString(input).byteInputStream()
}

abstract class AutoMarshallingJson : AutoMarshalling() {
    @Deprecated("Use asString instead", ReplaceWith("asString(input"))
    fun asJsonString(input: Any): String = asString(input)
}

abstract class JsonLibAutoMarshallingJson<NODE : Any> : AutoMarshallingJson(), Json<NODE> {
    override fun asString(input: Any): String = compact(asJsonObject(input))

    abstract fun asJsonObject(input: Any): NODE

    abstract fun <T : Any> asA(j: NODE, target: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): NODE = asJsonObject(this)

    @JvmName("nodeAsA")
    fun <T : Any> NODE.asA(target: KClass<T>): T = asA(this, target)
}

fun jsonHttpBodyLens(description: String? = null, contentNegotiation: ContentNegotiation = None) = httpBodyRoot(listOf(Meta(true, "body", ObjectParam, "body", description)), APPLICATION_JSON, contentNegotiation)
    .map({ it.payload.asString() }, { Body(it) })
