package org.reekwest.http.contract

import org.reekwest.http.contract.lens.Get
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies
import org.reekwest.http.core.header

typealias QueryLens<T> = org.reekwest.http.contract.lens.Lens<Request, T>

typealias HeaderLens<T> = org.reekwest.http.contract.lens.Lens<Request, T>

typealias PathLens<T> = org.reekwest.http.contract.lens.Lens<String, T>

object Query : org.reekwest.http.contract.lens.BiDiLensSpec<Request, String, String>("query",
    org.reekwest.http.contract.lens.Get { name, target -> target.queries(name).map { it ?: "" } },
    org.reekwest.http.contract.lens.Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)

object Header : org.reekwest.http.contract.lens.BiDiLensSpec<HttpMessage, String, String>("header",
    org.reekwest.http.contract.lens.Get { name, target -> target.headerValues(name).map { it ?: "" } },
    org.reekwest.http.contract.lens.Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val X_KONTRACT_ROUTE = org.reekwest.http.contract.Header.optional("x-http.contract-route")
        val CONTENT_TYPE = org.reekwest.http.contract.Header.map(::ContentType, { it.value }).optional("content-type")
    }
}

object Cookies : org.reekwest.http.contract.lens.BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    org.reekwest.http.contract.lens.Get { name, target -> target.cookies().filter { it.name == name } },
    org.reekwest.http.contract.lens.Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)

open class PathSpec<MID, out OUT>(internal val delegate: org.reekwest.http.contract.lens.LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): org.reekwest.http.contract.PathLens<OUT> {
        val getLens = delegate.get(name)
        return object : org.reekwest.http.contract.lens.Lens<String, OUT>(org.reekwest.http.contract.lens.Meta(true, "path", name, description), { getLens(it).firstOrNull() ?: throw org.reekwest.http.contract.lens.LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): org.reekwest.http.contract.PathSpec<MID, NEXT> = org.reekwest.http.contract.PathSpec(delegate.map(nextIn))
}

object Path : org.reekwest.http.contract.PathSpec<String, String>(org.reekwest.http.contract.lens.LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun fixed(name: String): org.reekwest.http.contract.PathLens<String> {
        val getLens = org.reekwest.http.contract.Path.delegate.get(name)
        return object : org.reekwest.http.contract.lens.Lens<String, String>(org.reekwest.http.contract.lens.Meta(true, "path", name),
            { getLens(it).let { if (it == listOf(name)) name else throw org.reekwest.http.contract.lens.LensFailure() } }) {
            override fun toString(): String = name
        }
    }
}

fun org.reekwest.http.contract.Path.int() = org.reekwest.http.contract.Path.map(String::toInt)
fun org.reekwest.http.contract.Path.long() = org.reekwest.http.contract.Path.map(String::toLong)
fun org.reekwest.http.contract.Path.double() = org.reekwest.http.contract.Path.map(String::toDouble)
fun org.reekwest.http.contract.Path.float() = org.reekwest.http.contract.Path.map(String::toFloat)
fun org.reekwest.http.contract.Path.boolean() = org.reekwest.http.contract.Path.map(::safeBooleanFrom)
fun org.reekwest.http.contract.Path.localDate() = org.reekwest.http.contract.Path.map(java.time.LocalDate::parse)
fun org.reekwest.http.contract.Path.dateTime() = org.reekwest.http.contract.Path.map(java.time.LocalDateTime::parse)
fun org.reekwest.http.contract.Path.zonedDateTime() = org.reekwest.http.contract.Path.map(java.time.ZonedDateTime::parse)
fun org.reekwest.http.contract.Path.uuid() = org.reekwest.http.contract.Path.map(java.util.UUID::fromString)


fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.int() = this.map(String::toInt, Int::toString)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.long() = this.map(String::toLong, Long::toString)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.double() = this.map(String::toDouble, Double::toString)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.float() = this.map(String::toFloat, Float::toString)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.boolean() = this.map(::safeBooleanFrom, Boolean::toString)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.localDate() = this.map(java.time.LocalDate::parse, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE::format)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.dateTime() = this.map(java.time.LocalDateTime::parse, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.zonedDateTime() = this.map(java.time.ZonedDateTime::parse, java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun <IN> org.reekwest.http.contract.lens.BiDiLensSpec<IN, String, String>.uuid() = this.map(java.util.UUID::fromString, java.util.UUID::toString)

internal fun safeBooleanFrom(value: String): Boolean =
    if (value.toUpperCase() == "TRUE") true
    else if (value.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
