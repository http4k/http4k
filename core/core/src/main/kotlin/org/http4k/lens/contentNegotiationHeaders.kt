package org.http4k.lens

import org.http4k.core.ContentEncodingName
import org.http4k.core.Method
import org.http4k.core.PriorityList
import org.http4k.core.fromSimpleRangeHeader
import org.http4k.core.toSimpleRangeHeader
import java.nio.charset.Charset
import java.util.Locale

// RFC 9110 Section 10.2.1
val Header.ALLOW get() = Header.enum<Method>().multi.required("Allow")

private data class PriorityListParam(val itemType: ParamMeta) :
    ParamMeta("priority list of " + itemType.description)

// RFC 9110 Section 12.5.2
val Header.ACCEPT_CHARSET by lazyOf(
    Header.mapWithNewMeta(
        { PriorityList.fromSimpleRangeHeader(it, Charset::forName) },
        { it.toSimpleRangeHeader { charset -> charset.name().lowercase() } },
        PriorityListParam(CharsetParam)
    ).optional("accept-charset")
)

private data object CharsetParam : ParamMeta("charset")

// RFC 9110 Section 12.5.3
val Header.ACCEPT_ENCODING by lazyOf(
    Header.mapWithNewMeta(
        { PriorityList.fromSimpleRangeHeader(it, ::contentEncodingName) },
        { it.toSimpleRangeHeader(ContentEncodingName::value) },
        PriorityListParam(ContentEncodingParam)
    ).optional("accept-encoding")
)

val Header.CONTENT_ENCODING by lazyOf(
    Header.mapWithNewMeta(
        ::contentEncodingName,
        { it.value },
        ContentEncodingParam
    ).optional("content-encoding")
)

// RFC 9110 Section 8.4.1.3: a recipient SHOULD consider "x-gzip" to be equivalent to "gzip"
private fun contentEncodingName(name: String) = when (val lower = name.lowercase()) {
    ContentEncodingName.X_GZIP.value -> ContentEncodingName.GZIP
    else -> ContentEncodingName(lower)
}

private data object ContentEncodingParam : ParamMeta("content encoding")

// RFC 9110, Section 12.5.4
// Supports language selection by the Basic Filtering scheme only (RFC 4647, Section 3.3.1)
val Header.ACCEPT_LANGUAGE by lazyOf(
    Header.mapWithNewMeta(
        { PriorityList.fromSimpleRangeHeader(it, Locale::forLanguageTag) },
        { it.toSimpleRangeHeader(Locale::toLanguageTag) },
        PriorityListParam(LanguageParam)
    ).optional("accept-language")
)

val Header.CONTENT_LANGUAGE by lazyOf(
    Header.mapWithNewMeta(
        Locale::forLanguageTag, Locale::toLanguageTag,
        LanguageParam
    ).optional("content-language")
)

private data object LanguageParam : ParamMeta("language")
