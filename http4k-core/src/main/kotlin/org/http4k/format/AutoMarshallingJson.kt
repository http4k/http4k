package org.http4k.format

import kotlin.reflect.KClass

abstract class AutoMarshallingJson {

    abstract fun asJsonString(a: Any): String

    abstract fun <T : Any> asA(s: String, c: KClass<T>): T

    @JvmName("stringAsA")
    fun <T : Any> String.asA(c: KClass<T>): T = asA(this, c)
}

abstract class JsonLibAutoMarshallingJson<NODE : Any> : AutoMarshallingJson(), Json<NODE> {
    override fun asJsonString(a: Any): String = compact(asJsonObject(a))

    abstract fun asJsonObject(a: Any): NODE

    abstract fun <T : Any> asA(j: NODE, c: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): NODE = asJsonObject(this)

    @JvmName("nodeAsA")
    fun <T : Any> NODE.asA(c: KClass<T>): T = asA(this, c)
}