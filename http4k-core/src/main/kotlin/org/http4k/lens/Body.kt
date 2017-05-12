package org.http4k.lens

import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.with
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.StringParam
import java.nio.ByteBuffer
import java.util.Collections.emptyList

open class BodyLens<out FINAL>(val metas: List<Meta>, private val get: (HttpMessage) -> FINAL) : (HttpMessage) -> FINAL {

    @Throws(LensFailure::class)
    override operator fun invoke(target: HttpMessage): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(metas.map(::Invalid))
    }
}

class BiDiBodyLens<FINAL>(metas: List<Meta>,
                          get: (HttpMessage) -> FINAL,
                          private val set: (FINAL, HttpMessage) -> HttpMessage) : BodyLens<FINAL>(metas, get) {

    @Suppress("UNCHECKED_CAST")
    operator fun <R : HttpMessage> invoke(value: FINAL, target: R): R = set(value, target) as R

    infix fun <R : HttpMessage> to(value: FINAL): (R) -> R = { invoke(value, it) }
}


open class BodyLensSpec<MID, out OUT>(internal val metas: List<Meta>, internal val get: LensGet<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): BodyLens<OUT> {
        val getLens = get("")
        return BodyLens(metas, { getLens(it).firstOrNull() ?: throw LensFailure(metas.map(::Missing)) })
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodyLensSpec<MID, NEXT> = BodyLensSpec(metas, get.map(nextIn))
}

open class BiDiBodyLensSpec<MID, OUT>(metas: List<Meta>,
                                      get: LensGet<HttpMessage, MID, OUT>,
                                      private val set: LensSet<HttpMessage, MID, OUT>) : BodyLensSpec<MID, OUT>(metas, get) {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiBodyLensSpec(metas, get.map(nextIn), set.map(nextOut))

    override fun required(description: String?): BiDiBodyLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiBodyLens(metas,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(metas.map(::Missing)) else it.first() } },
            { out: OUT, target: HttpMessage -> setLens(out?.let { listOf(it) } ?: kotlin.collections.emptyList(), target) }
        )
    }
}

internal fun root(metas: List<Meta>, contentType: ContentType) = BiDiBodyLensSpec<ByteBuffer, ByteBuffer>(metas,
    LensGet { _, target ->
        if (CONTENT_TYPE(target) != contentType) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
        target.body?.let { listOf(it.payload) } ?: emptyList()
    },
    LensSet { _, values, target -> values.fold(target) { a, b -> a.body(org.http4k.core.Body(b)) }.with(CONTENT_TYPE to contentType) }
)


fun Body.Companion.string(contentType: ContentType, description: String? = null): BiDiBodyLensSpec<ByteBuffer, String>
    = root(listOf(Meta(true, "body", StringParam, "body", description)), contentType).map(ByteBuffer::asString, String::asByteBuffer)

fun Body.Companion.binary(contentType: ContentType, description: String? = null): BiDiBodyLensSpec<ByteBuffer, ByteBuffer>
    = root(listOf(Meta(true, "body", FileParam, "body", description)), contentType)
