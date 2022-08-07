package org.http4k.format

import org.http4k.core.Accept
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Header
import org.http4k.lens.LensExtractor

class AutoContentNegotiator<T>(
    private val defaultLens: BiDiBodyLens<T>,
    private val alternateLenses: List<BiDiBodyLens<T>>
): Iterable<BiDiBodyLens<T>>, LensExtractor<HttpMessage, T> {

    override fun iterator() = listOf(defaultLens).plus(alternateLenses).iterator()

    operator fun invoke(accept: Accept?): BiDiBodyLens<T>
        = find { accept == null || accept.accepts(it.contentType) } ?: defaultLens

    override fun invoke(target: HttpMessage): T = Header
        .CONTENT_TYPE(target)
        ?.let { contentType -> Accept(listOf(contentType), emptyList()) }
        .let { accept -> invoke(accept) }
        .invoke(target)

    fun outbound(request: Request): BiDiBodyLens<T> = invoke(Header.ACCEPT(request))

    fun toBodyLens() = BodyLens(
        metas = defaultLens.metas + alternateLenses.flatMap { it.metas },
        contentType = defaultLens.contentType,
        getLens = this::invoke
    )
}

fun <T> ContentNegotiation.Companion.auto(
    defaultLens: BiDiBodyLens<T>,
    vararg lenses: BiDiBodyLens<T>
) = AutoContentNegotiator(defaultLens, lenses.toList())

