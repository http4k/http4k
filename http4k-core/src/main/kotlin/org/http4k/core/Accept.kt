package org.http4k.core

data class Accept(val contentTypes: List<ContentType>, val directives: Parameters) {

    /**
     * Note that the Accept header ignores CharSet because that is in a separate Accept-CharSet header..
     */
    fun accepts(contentType: ContentType): Boolean = contentTypes.any { it.equalsIgnoringDirectives(contentType)}
}
