package org.http4k.format

import kotlin.reflect.KClass

abstract class AutoMarshallingJson<ROOT : Any> {

    abstract fun asJsonString(a: Any): String

    abstract fun asJsonObject(a: Any): ROOT

    abstract fun parse(s: String): ROOT

    abstract fun <T : Any> asA(s: String, c: KClass<T>): T

    abstract fun <T : Any> asA(j: ROOT, c: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): ROOT = asJsonObject(this)

    @JvmName("stringAsA")
    fun <T : Any> String.asA(c: KClass<T>): T = asA(this, c)

    @JvmName("nodeAsA")
    fun <T : Any> ROOT.asA(c: KClass<T>): T = asA(this, c)
}

abstract class JsonLibAutoMarshallingJson<ROOT : Any> : AutoMarshallingJson<ROOT>(), Json<ROOT, ROOT> {
    override fun asJsonString(a: Any): String = compact(asJsonObject(a))
}