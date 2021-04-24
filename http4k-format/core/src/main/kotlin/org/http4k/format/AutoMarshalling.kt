package org.http4k.format

import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Common base type for all format libraries which can convert directly from String -> Classes
 */
abstract class AutoMarshalling {

    abstract fun <T : Any> asA(input: String, target: KClass<T>): T

    abstract fun <T : Any> asA(input: InputStream, target: KClass<T>): T

    @JvmName("streamAsA")
    inline fun <reified T : Any> asA(input: InputStream): T = asA(input, T::class)

    @JvmName("stringAsA")
    inline fun <reified T : Any> asA(input: String): T = asA(input, T::class)

    @JvmName("stringAsA")
    fun <T : Any> String.asA(target: KClass<T>): T = asA(this, target)

    abstract fun asFormatString(input: Any): String

    fun asInputStream(input: Any): InputStream = asFormatString(input).byteInputStream()

    /**
     * Conversion happens by converting the base object into JSON and then out again
     */
    inline fun <IN: Any, reified OUT : Any> convert(input: IN): OUT = asA(asFormatString(input))
}
