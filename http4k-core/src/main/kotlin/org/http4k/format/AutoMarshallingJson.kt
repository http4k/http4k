package org.http4k.format

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.httpBodyRoot
import java.io.InputStream
import kotlin.reflect.KClass

abstract class AutoMarshallingJson {
    abstract fun asJsonString(input: Any): String

    abstract fun <T : Any> asA(input: String, target: KClass<T>): T

    @JvmName("stringAsA")
    fun <T : Any> String.asA(target: KClass<T>): T = asA(this, target)

    fun asInputStream(input: Any): InputStream = asJsonString(input).byteInputStream()
}

abstract class JsonLibAutoMarshallingJson<NODE : Any> : AutoMarshallingJson(), Json<NODE> {
    override fun asJsonString(input: Any): String = compact(asJsonObject(input))

    abstract fun asJsonObject(input: Any): NODE

    abstract fun <T : Any> asA(j: NODE, target: KClass<T>): T

    inline fun <reified T : Any> NODE.asA(): T = asA(this, T::class)

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): NODE = asJsonObject(this)

    @JvmName("nodeAsA")
    fun <T : Any> NODE.asA(target: KClass<T>): T = asA(this, target)
}

fun jsonHttpBodyLens(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None) = httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_JSON, contentNegotiation)
    .map({ it.payload.asString() }, { Body(it) })
