package org.http4k.format

import kotlin.reflect.KClass

abstract class AutoMarshallingJson<ROOT : Any> : Json<ROOT, ROOT> {
    abstract fun asJsonObject(any: Any): ROOT

    abstract fun <T : Any> asA(s: String, c: KClass<T>): T

    abstract fun <T : Any> asA(s: ROOT, c: KClass<T>): T

    @JvmName("anyAsJsonObject")
    fun Any.asJsonObject(): ROOT = asJsonObject(this)

    @JvmName("stringAsA")
    fun <T : Any> String.asA(c: KClass<T>): T = asA(this, c)

    @JvmName("nodeAsA")
    fun <T : Any> ROOT.asA(c: KClass<T>): T = asA(this, c)
}