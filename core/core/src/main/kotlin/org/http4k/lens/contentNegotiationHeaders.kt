package org.http4k.lens

import org.http4k.core.ContentEncodingName
import org.http4k.core.PriorityList
import org.http4k.core.fromSimpleRangeHeader
import org.http4k.core.toSimpleRangeHeader
import org.http4k.lens.Header.map
import java.nio.charset.Charset
import java.util.Locale


// RFC 9110 Section 12.5.2
val Header.ACCEPT_CHARSET by lazyOf(
    map(
        { PriorityList.fromSimpleRangeHeader(it, Charset::forName) },
        { it.toSimpleRangeHeader { charset -> charset.name().lowercase() } }
    ).optional("accept-charset")
)

// RFC 9110 Section 12.5.3
val Header.ACCEPT_ENCODING by lazyOf(
    map(
        { PriorityList.fromSimpleRangeHeader(it, ::ContentEncodingName) },
        { it.toSimpleRangeHeader(ContentEncodingName::value) }
    ).optional("accept-encoding")
)

val Header.CONTENT_ENCODING by lazyOf(
    map(::ContentEncodingName, { it.value }).optional("content-encoding")
)


// RFC 9110, Section 12.5.4
// Supports language selection by the Basic Filtering scheme only (RFC 4647, Section 3.3.1)
val Header.ACCEPT_LANGUAGE by lazyOf(
    map({ PriorityList.fromSimpleRangeHeader(it, Locale::forLanguageTag) },
        { it.toSimpleRangeHeader(Locale::toLanguageTag) }
    ).optional("accept-language")
)

val Header.CONTENT_LANGUAGE by lazyOf(
    map(Locale::forLanguageTag, Locale::toLanguageTag).optional("content-language")
)
