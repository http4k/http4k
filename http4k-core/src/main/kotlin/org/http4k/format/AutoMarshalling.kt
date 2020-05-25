package org.http4k.format

import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Common base type for all format libraries which can convert directly from String -> Classes
 */
abstract class AutoMarshalling {
    abstract fun <T : Any> asA(input: String, target: KClass<T>): T

    @JvmName("stringAsA")
    inline fun <reified T : Any> asA(input: String): T = asA(input, T::class)

    @JvmName("stringAsA")
    fun <T : Any> String.asA(target: KClass<T>): T = asA(this, target)

    abstract fun asFormatString(input: Any): String

    fun asInputStream(input: Any): InputStream = asFormatString(input).byteInputStream()
}
