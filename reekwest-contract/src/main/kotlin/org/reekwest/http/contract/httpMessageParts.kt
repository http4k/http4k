package org.reekwest.http.contract

import org.reekwest.http.contract.lens.BiDiLensSpec
import org.reekwest.http.contract.lens.Get
import org.reekwest.http.contract.lens.Lens
import org.reekwest.http.contract.lens.LensFailure
import org.reekwest.http.contract.lens.LensSpec
import org.reekwest.http.contract.lens.Meta
import org.reekwest.http.contract.lens.Set
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies
import org.reekwest.http.core.header
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

typealias QueryLens<T> = Lens<Request, T>

typealias HeaderLens<T> = Lens<Request, T>

typealias PathLens<T> = Lens<String, T>

object Query : BiDiLensSpec<Request, String, String>("query",
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    Get { name, target -> target.headerValues(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val X_KONTRACT_ROUTE = Header.optional("x-http.contract-route")
        val CONTENT_TYPE = Header.map(::ContentType, { it.value }).optional("content-type")
    }
}

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    Get { name, target -> target.cookies().filter { it.name == name } },
    Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)

open class PathSpec<MID, out OUT>(internal val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> {
        val getLens = delegate.get(name)
        return object : Lens<String, OUT>(Meta(true, "path", name, description), { getLens(it).firstOrNull() ?: throw LensFailure() }) {
            override fun toString(): String = "{$name}"
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))
}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun fixed(name: String): PathLens<String> {
        val getLens = delegate.get(name)
        return object : Lens<String, String>(Meta(true, "path", name),
            { getLens(it).let { if (it == listOf(name)) name else throw LensFailure() } }) {
            override fun toString(): String = name
        }
    }
}

fun Path.int() = map(String::toInt)
fun Path.long() = map(String::toLong)
fun Path.double() = map(String::toDouble)
fun Path.float() = map(String::toFloat)
fun Path.boolean() = map(::safeBooleanFrom)
fun Path.localDate() = map(LocalDate::parse)
fun Path.dateTime() = map(LocalDateTime::parse)
fun Path.zonedDateTime() = map(ZonedDateTime::parse)
fun Path.uuid() = map(UUID::fromString)


fun <IN> BiDiLensSpec<IN, String, String>.int() = this.map(String::toInt, Int::toString)
fun <IN> BiDiLensSpec<IN, String, String>.long() = this.map(String::toLong, Long::toString)
fun <IN> BiDiLensSpec<IN, String, String>.double() = this.map(String::toDouble, Double::toString)
fun <IN> BiDiLensSpec<IN, String, String>.float() = this.map(String::toFloat, Float::toString)
fun <IN> BiDiLensSpec<IN, String, String>.boolean() = this.map(::safeBooleanFrom, Boolean::toString)
fun <IN> BiDiLensSpec<IN, String, String>.localDate() = this.map(LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE::format)
fun <IN> BiDiLensSpec<IN, String, String>.dateTime() = this.map(LocalDateTime::parse, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String, String>.zonedDateTime() = this.map(ZonedDateTime::parse, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
fun <IN> BiDiLensSpec<IN, String, String>.uuid() = this.map(UUID::fromString, UUID::toString)

internal fun safeBooleanFrom(value: String): Boolean =
    if (value.toUpperCase() == "TRUE") true
    else if (value.toUpperCase() == "FALSE") false
    else throw kotlin.IllegalArgumentException("illegal boolean")
