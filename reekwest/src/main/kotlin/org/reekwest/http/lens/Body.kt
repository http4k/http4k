package org.reekwest.http.lens

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.copy
import org.reekwest.http.core.with
import org.reekwest.http.lens.Header.Common.CONTENT_TYPE
import java.nio.ByteBuffer
import java.util.Collections.emptyList

open class BoddyLens<out FINAL>(val metas: List<Meta>, private val get: (HttpMessage) -> FINAL) : (HttpMessage) -> FINAL {

    @Throws(LensFailure::class)
    override operator fun invoke(target: HttpMessage): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(metas.map(::Invalid))
    }
}

class BiDiBoddyLens<FINAL>(metas: List<Meta>,
                           get: (HttpMessage) -> FINAL,
                           private val set: (FINAL, HttpMessage) -> HttpMessage) : BoddyLens<FINAL>(metas, get) {

    @Suppress("UNCHECKED_CAST")
    operator fun <R : HttpMessage> invoke(value: FINAL, target: R): R = set(value, target) as R

    infix fun <R : HttpMessage> to(value: FINAL): (R) -> R = { invoke(value, it) }
}


open class BoddyLensSpec<MID, out OUT>(internal val metas: List<Meta>, internal val get: Get<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): BoddyLens<OUT> {
        val getLens = get("")
        return BoddyLens(metas, { getLens(it).firstOrNull() ?: throw LensFailure(metas.map(::Missing)) })
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): BoddyLensSpec<MID, NEXT> = BoddyLensSpec(metas, get.map(nextIn))
}

open class BiDiBoddyLensSpec<MID, OUT>(metas: List<Meta>,
                                       get: Get<HttpMessage, MID, OUT>,
                                       private val set: Set<HttpMessage, MID, OUT>) : BoddyLensSpec<MID, OUT>(metas, get) {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiBoddyLensSpec(metas, get.map(nextIn), set.map(nextOut))

    override fun required(description: String?): BiDiBoddyLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiBoddyLens(metas,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(metas.map(::Missing)) else it.first() } },
            { out: OUT, target: HttpMessage -> setLens(out?.let { listOf(it) } ?: kotlin.collections.emptyList(), target) }
        )
    }
}

typealias BodyLens<T> = Lens<HttpMessage, T>
typealias BiDiBodyLens<T> = BiDiLens<HttpMessage, T>

open class BodySpec<MID, out OUT>(private val delegate: LensSpec<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): BodyLens<OUT> = delegate.required("body", description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodySpec<MID, NEXT> = BodySpec(delegate.map(nextIn))
}

open class BiDiBodySpec<MID, OUT>(private val delegate: BiDiLensSpec<HttpMessage, MID, OUT>) : BodySpec<MID, OUT>(delegate) {
    override fun required(description: String?): BiDiBodyLens<OUT> = delegate.required("body", description)

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiBodySpec<MID, NEXT> = BiDiBodySpec(delegate.map(nextIn, nextOut))
}

object Body {
    fun root(metas: List<Meta>, contentType: ContentType)= BiDiBoddyLensSpec<ByteBuffer, ByteBuffer>(metas,
        Get { _, target ->
            if (CONTENT_TYPE(target) != contentType) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
            target.body?.let { listOf(it) } ?: emptyList()
        },
        Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) }.with(CONTENT_TYPE to contentType) }
    )

    fun binary(contentType: ContentType) = BiDiBodySpec<ByteBuffer, ByteBuffer>(BiDiLensSpec("body",
        Get { _, target ->
            if (CONTENT_TYPE(target) != contentType) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
            target.body?.let { listOf(it) } ?: emptyList()
        },
        Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) }.with(CONTENT_TYPE to contentType) }
    ))

    fun string(contentType: ContentType): BiDiBodySpec<ByteBuffer, String>
        = binary(contentType).map(ByteBuffer::asString, String::asByteBuffer)

    fun string2(contentType: ContentType, description: String? = null): BiDiBoddyLensSpec<ByteBuffer, String>
        = root(listOf(Meta(true, "body", "body", description)), contentType).map(ByteBuffer::asString, String::asByteBuffer)

    fun binary2(contentType: ContentType, description: String? = null): BiDiBoddyLensSpec<ByteBuffer, ByteBuffer>
        = root(listOf(Meta(true, "body", "body", description)), contentType)
}

