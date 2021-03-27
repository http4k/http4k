package org.http4k.lens

import org.http4k.core.Accept
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Parameters
import org.http4k.core.Uri
import org.http4k.core.Uri.Companion.of
import org.http4k.lens.ParamMeta.StringParam

typealias HeaderLens<T> = Lens<HttpMessage, T>

object Header : BiDiLensSpec<HttpMessage, String>("header", StringParam,
    LensGet { name, target -> target.headerValues(name).map { it ?: "" } },
    LensSet { name, values, target -> values.fold(target.removeHeader(name)) { m, next -> m.header(name, next) } }
) {
    val CONTENT_TYPE = map(
        {
            parseValueAndDirectives(it).let {
                ContentType(it.first, it.second
                    .filter { it.first.toLowerCase() in setOf("boundary", "charset", "media-type") }
                )
            }
        },
        ContentType::toHeaderValue
    ).optional("content-type")

    val LOCATION = map(::of, Uri::toString).required("location")

    val ACCEPT = map {
        parseValueAndDirectives(it).let {
            Accept(it.first.split(",").map { it.trim() }.map(::ContentType), it.second)
        }
    }.optional("Accept")

    internal fun parseValueAndDirectives(it: String): Pair<String, Parameters> =
        with(it.split(";").mapNotNull { it.trim().takeIf(String::isNotEmpty) }) {
            first() to drop(1).map {
                with(it.split("=")) {
                    first() to if (size == 1) null else drop(1).joinToString("=")
                }
            }
        }
}
