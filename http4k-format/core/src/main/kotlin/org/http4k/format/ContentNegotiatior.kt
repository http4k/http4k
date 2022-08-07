package org.http4k.format

import org.http4k.core.Accept
import org.http4k.core.Request
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Header

class ContentNegotiator<T>(
    private val defaultLens: BiDiBodyLens<T>,
    private val lenses: List<BiDiBodyLens<T>>
): Iterable<BiDiBodyLens<T>> {

    override fun iterator() = sequenceOf(defaultLens, *lenses.toTypedArray()).iterator()

    operator fun invoke(accept: Accept?): BiDiBodyLens<T>
        = find { accept == null || accept.accepts(it.contentType) } ?: defaultLens

    operator fun invoke(request: Request): T = Header
        .CONTENT_TYPE(request)
        ?.let { contentType -> Accept(listOf(contentType), emptyList()) }
        .let { accept -> invoke(accept) }
        .invoke(request)

    fun accepting(request: Request): BiDiBodyLens<T> = invoke(Header.ACCEPT(request))
}

fun <OUT> ContentNegotiation.Companion.Negotiator(
    defaultLens: BiDiBodyLens<OUT>,
    vararg lenses: BiDiBodyLens<OUT>
) = ContentNegotiator(defaultLens, lenses.toList())

