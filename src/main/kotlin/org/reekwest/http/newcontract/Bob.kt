package org.reekwest.http.newcontract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.contract.*
import org.reekwest.http.contract.ContractBreach.Companion.Invalid
import org.reekwest.http.contract.ContractBreach.Companion.Missing
import org.reekwest.http.core.*
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.newcontract.Header.Common.CONTENT_TYPE
import java.net.URLDecoder
import java.nio.ByteBuffer

class MappableGetLens<in IN, MID, out OUT>(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String): GetLens<IN, OUT> = object : GetLens<IN, OUT> {
        override fun invoke(target: IN): List<OUT> = rootFn(name, target).map(fn)
    }

    fun <NEXT> map(nextFn: (OUT) -> NEXT): MappableGetLens<IN, MID, NEXT> = MappableGetLens(rootFn, { nextFn(fn(it)) })
}

class MappableSetLens<IN, MID, in OUT>(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String): SetLens<IN, OUT> = object : SetLens<IN, OUT> {
        override fun invoke(values: List<OUT>, target: IN): IN = rootFn(name, values.map(fn), target)
    }

    fun <NEXT> map(nextFn: (NEXT) -> OUT): MappableSetLens<IN, MID, NEXT> = MappableSetLens(rootFn, { fn(nextFn(it)) })
}

interface MultiGetLensSpec<in IN, OUT> {
    fun optional(name: String, description: String? = null): MetaLens<IN, OUT, List<OUT>?>
    fun required(name: String, description: String? = null): MetaLens<IN, OUT, List<OUT>>
}

open class GetLensSpec<IN, MID, OUT>(internal val location: String, internal val createGetLens: MappableGetLens<IN, MID, OUT>) {
    fun <NEXT> map(nextIn: (OUT) -> NEXT): GetLensSpec<IN, MID, NEXT> = GetLensSpec(location, createGetLens.map(nextIn))

    open fun optional(name: String, description: String? = null): MetaLens<IN, OUT, OUT?> =
        object : MetaLens<IN, OUT, OUT?>(Meta(name, location, false, description), createGetLens(name)) {
            override fun convertIn(o: List<OUT>): OUT? = o.firstOrNull()
        }

    open fun required(name: String, description: String? = null): MetaLens<IN, OUT, OUT> =
        object : MetaLens<IN, OUT, OUT>(Meta(name, location, false, description), createGetLens(name)) {
            override fun convertIn(o: List<OUT>): OUT = o.firstOrNull() ?: throw Missing(this)
        }

    open val multi = object : MultiGetLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): MetaLens<IN, OUT, List<OUT>?> =
            object : MetaLens<IN, OUT, List<OUT>?>(Meta(name, location, false, description), createGetLens(name)) {
                override fun convertIn(o: List<OUT>): List<OUT>? = if (o.isEmpty()) null else o
            }

        override fun required(name: String, description: String?): MetaLens<IN, OUT, List<OUT>> =
            object : MetaLens<IN, OUT, List<OUT>>(Meta(name, location, false, description), createGetLens(name)) {
                override fun convertIn(o: List<OUT>): List<OUT> = if (o.isEmpty()) throw Missing(this) else o
            }
    }
}

interface BiDiMultiLensSpec<IN, OUT> : MultiGetLensSpec<IN, OUT> {
    override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>?>
    override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>>
}

open class BiDiLensSpec<IN, MID, OUT>(location: String, createGetLens: MappableGetLens<IN, MID, OUT>,
                                      private val createSetLens: MappableSetLens<IN, MID, OUT>) : GetLensSpec<IN, MID, OUT>(location, createGetLens) {

    private fun biDiLensFor(name: String): BiDiLens<IN, OUT> {
        val getLens = createGetLens(name)
        val setLens = createSetLens(name)

        return object : BiDiLens<IN, OUT> {
            override fun invoke(target: IN): List<OUT> = getLens(target)
            override fun invoke(values: List<OUT>, target: IN): IN = setLens(values, target)
        }
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiLensSpec<IN, MID, NEXT> =
        BiDiLensSpec(location,
            createGetLens.map(nextIn),
            createSetLens.map(nextOut))

    override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, OUT?> =
        object : BiDiMetaLens<IN, OUT, OUT?>(Meta(name, location, false, description), biDiLensFor(name)) {
            override fun convertIn(o: List<OUT>): OUT? = o.firstOrNull()
            override fun convertOut(o: OUT?): List<OUT> = o?.let { listOf(it) } ?: emptyList()
        }

    override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, OUT> =
        object : BiDiMetaLens<IN, OUT, OUT>(Meta(name, location, true, description), biDiLensFor(name)) {
            override fun convertIn(o: List<OUT>): OUT = o.firstOrNull() ?: throw Missing(this)
            override fun convertOut(o: OUT): List<OUT> = listOf(o)
        }

    override val multi = object : BiDiMultiLensSpec<IN, OUT> {
        override fun optional(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>?> =
            object : BiDiMetaLens<IN, OUT, List<OUT>?>(Meta(name, location, false, description), biDiLensFor(name)) {
                override fun convertOut(o: List<OUT>?): List<OUT> = o ?: emptyList()
                override fun convertIn(o: List<OUT>): List<OUT>? = if (o.isEmpty()) null else o
            }

        override fun required(name: String, description: String?): BiDiMetaLens<IN, OUT, List<OUT>> =
            object : BiDiMetaLens<IN, OUT, List<OUT>>(Meta(name, location, false, description), biDiLensFor(name)) {
                override fun convertOut(o: List<OUT>): List<OUT> = o
                override fun convertIn(o: List<OUT>): List<OUT> = if (o.isEmpty()) throw Missing(this) else o
            }
    }
}

object Query : BiDiLensSpec<Request, String, String>("query",
    MappableGetLens({ name, target -> target.queries(name).map { it ?: "" } }, { it }),
    MappableSetLens({ name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }, { it })
)

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    MappableGetLens({ name, target -> target.cookie(name)?.let { listOf(it) } ?: emptyList() }, { it }),
    MappableSetLens({ _, values, target -> values.fold(target, { m, next -> m.header("Cookie", next.toString()) }) }, { it })
)

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    MappableGetLens({ name, target -> target.headerValues(name).map { it ?: "" } }, { it }),
    MappableSetLens({ name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }, { it })
) {
    object Common {
        val CONTENT_TYPE = Header.map(::ContentType).optional("Content-Type")
    }
}

object Body : BiDiLensSpec<HttpMessage, ByteBuffer, ByteBuffer>("body",
    MappableGetLens({ _, target -> target.body?.let { listOf(it) } ?: emptyList() }, { it }),
    MappableSetLens({ _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }, { it })
) {
    val string = Body.map(ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null) = string.required("body", description)
}

fun Body.form() = BiDiLensSpec<Request, WebForm, WebForm>("form",
    MappableGetLens({ _, target ->
        if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw Invalid(CONTENT_TYPE)
        listOf(WebForm(formParametersFrom(target), emptyList()))
    }, { it }),
    MappableSetLens({ _, values, target ->
        //FIXME this doesn't work!
        target
    }, { it })
).required("form")

private fun formParametersFrom(target: Request): Map<String, List<String>> {
    return target.bodyString()
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { URLDecoder.decode(it[0], "UTF-8") to if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
}

object FormField : BiDiLensSpec<WebForm, String, String>("header",
    MappableGetLens({ name, (fields) -> fields.getOrDefault(name, listOf()) }, { it }),
    MappableSetLens({ name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }, { it })
)

fun <IN> BiDiLensSpec<IN, String, String>.int(): BiDiLensSpec<IN, String, Int> = this.map(String::toInt, Int::toString)
fun <IN> BiDiLensSpec<IN, String, String>.long(): BiDiLensSpec<IN, String, Long> = this.map(String::toLong, Long::toString)
