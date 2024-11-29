package org.http4k.lens

import org.http4k.core.Accept
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Credentials
import org.http4k.core.HttpMessage
import org.http4k.core.Parameters
import org.http4k.core.QualifiedContent
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.Uri.Companion.of
import org.http4k.core.findSingle
import org.http4k.core.with
import org.http4k.lens.Header.ACCEPT
import org.http4k.lens.Header.AUTHORIZATION_BASIC
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Header.LOCATION
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.StringParam
import java.util.Locale.getDefault

typealias HeaderLens<T> = Lens<HttpMessage, T>


object Header : BiDiLensSpec<HttpMessage, String>("header", StringParam,
    LensGet { name, target -> target.headerValues(name).map { it ?: "" } },
    LensSet { name, values, target -> values.fold(target.removeHeader(name)) { m, next -> m.header(name, next) } }
) {
    val CONTENT_TYPE = map(
        {
            parseValueAndDirectives(it).let {
                ContentType(it.first, it.second
                    .filter { it.first.lowercase(getDefault()) in setOf("boundary", "charset", "media-type") }
                )
            }
        },
        ContentType::toHeaderValue
    ).optional("content-type")

    val LOCATION = map(::of, Uri::toString).required("location")

    val ACCEPT = map(::parseAcceptContentHeader, ::injectAcceptContentHeaders).optional("Accept")

    private fun parseAcceptContentHeader(value: String): Accept =
        value.split(",")
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .map { rawContentType ->
                parseValueAndDirectives(rawContentType)
                    .let { it.toAcceptContentType() to it.second.findQValue()}
            }
            .map { QualifiedContent(it.first, it.second) }
            .let(::Accept)

    private fun injectAcceptContentHeaders(accept: Accept): String = accept.contentTypes.joinToString(", ") {
        it.content.withoutCharset().toHeaderValue() + it.priority
            .takeIf { priority -> priority < 1.0 }
            ?.let { priority -> "; q=$priority" }
            .orEmpty()
    }

    private fun Parameters.findQValue(): Double = findSingle("q")?.toDoubleOrNull() ?: 1.0

    val LINK = map(
        {
            Regex("""<(.+?)>; rel="(.+?)"""")
                .findAll(it)
                .map {
                    it.destructured.match.groupValues[2] to
                        of(it.destructured.match.groupValues[1])
                }.toMap()
        },
        {
            it.map { """<${it.value}>; rel="${it.key}"""" }.joinToString(", ")
        }
    )
        .defaulted("Link", emptyMap())

    val AUTHORIZATION_BASIC = basicCredentials().optional("Authorization")

    internal fun parseValueAndDirectives(it: String): Pair<String, Parameters> =
        with(it.split(";").mapNotNull { it.trim().takeIf(String::isNotEmpty) }) {
            first() to drop(1).map {
                with(it.split("=")) {
                    first() to if (size == 1) null else drop(1).joinToString("=")
                }
            }
        }
}

private fun Pair<String, Parameters>.toAcceptContentType() =
    ContentType(first, second.filter { it.first.lowercase(getDefault()) != "q" }).withoutCharset()


inline fun <reified T : Enum<T>> Header.enum(caseSensitive: Boolean = true) = mapWithNewMeta(
    if (caseSensitive) StringBiDiMappings.enum<T>() else StringBiDiMappings.caseInsensitiveEnum(),
    EnumParam(T::class)
)

fun HttpMessage.contentType(): ContentType? = CONTENT_TYPE(this)

fun <T : HttpMessage> T.contentType(contentType: ContentType): T = with(CONTENT_TYPE of contentType)

fun Response.location() = LOCATION(this)

fun Response.location(uri: Uri) = with(LOCATION of uri)

fun Request.accept(): Accept? = ACCEPT(this)

fun Request.accept(accept: Accept) = with(ACCEPT of accept)

fun Request.accept(contentType: ContentType) = with(ACCEPT of Accept(listOf(QualifiedContent(contentType))))

fun Request.basicAuthentication() = AUTHORIZATION_BASIC(this)

fun Request.basicAuthentication(credentials: Credentials) = with(AUTHORIZATION_BASIC of credentials)

fun Request.bearerAuth(token: String, scheme: String = "Bearer") = header("Authorization", "$scheme $token")

fun Request.bearerToken(): String? = header("Authorization")
    ?.trim()
    ?.takeIf { it.startsWith("Bearer ", true) }
    ?.substringAfter("earer ")

fun Response.html(body: String) = contentType(TEXT_HTML).body(body)
