package org.http4k.format

import kotlin.reflect.KClass

abstract class AutoMarshallingJson {

    abstract fun asJsonString(a: Any): String

    abstract fun <T : Any> asA(s: String, c: KClass<T>): T

    @JvmName("stringAsA")
    fun <T : Any> String.asA(c: KClass<T>): T = asA(this, c)
}

abstract class JsonLibAutoMarshallingJson<ROOT : Any> : AutoMarshallingJson(), Json<ROOT, ROOT> {
    override fun asJsonString(a: Any): String = compact(asJsonObject(a))

    abstract fun asJsonObject(a: Any): ROOT

    abstract fun <T : Any> asA(j: ROOT, c: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): ROOT = asJsonObject(this)

    @JvmName("nodeAsA")
    fun <T : Any> ROOT.asA(c: KClass<T>): T = asA(this, c)
}