package org.http4k.core

data class Accept(val contentTypes: List<ContentType>, val directives: Parameters) {

    /**
     * Note that the Accept header ignores CharSet because that is in a separate Accept-CharSet header..
     */
    fun accepts(contentType: ContentType): Boolean = contentTypes.any { it.equalsIgnoringDirectives(contentType) }
}

/**
 * See https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
 */
data class AcceptContent(val types: List<QualifiedContent>) {

    fun accepts(contentType: ContentType): Boolean = types.any { it.content.equalsIgnoringDirectives(contentType) }
}

data class QualifiedContent(val content: ContentType, val priority: Double = 1.0)
