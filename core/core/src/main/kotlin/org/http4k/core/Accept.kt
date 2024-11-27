package org.http4k.core

/**
 * See https://www.rfc-editor.org/rfc/rfc9110.html#name-accept
 */
data class Accept(val contentTypes: List<QualifiedContent>) {

    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Directives cannot be used in this context", ReplaceWith("Accept(types)"))
    constructor(contentTypes: List<ContentType>, directives: Parameters) :
        this(contentTypes.map { QualifiedContent(it) })

    /**
     * Note that the Accept header ignores CharSet because that is in a separate Accept-CharSet header..
     */
    fun accepts(contentType: ContentType): Boolean =
        contentTypes.any { it.content.equalsIgnoringDirectives(contentType) }
}

data class QualifiedContent(val content: ContentType, val priority: Double = 1.0)
