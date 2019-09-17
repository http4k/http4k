package org.http4k.format

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