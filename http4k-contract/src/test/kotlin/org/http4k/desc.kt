package org.http4k

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.HeaderLens
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.QueryLens
import org.http4k.contract.ContractRoutingHttpHandler.Companion.Handler as ContractHandler

data class Tag(val name: String, val description: String? = null)

class Desc private constructor(internal val core: Core) {
    constructor(name: String = "<unknown>", description: String? = null) : this(Core(name, description, null))

    fun header(new: HeaderLens<*>) = Desc(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun query(new: QueryLens<*>) = Desc(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun body(new: BodyLens<*>) = Desc(core.copy(body = new, consumes = core.consumes.plus(new.contentType)))
    fun <T> body(new: Pair<BiDiBodyLens<T>, T>): Desc = Desc(core.copy(request = Request(GET, "").with(new.first of new.second))).body(new.first)

    fun taggedWith(tag: String) = taggedWith(Tag(tag))
    fun taggedWith(vararg tags: Tag) = Desc(core.copy(tags = core.tags.plus(tags)))

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) =
        Desc(core.copy(
            produces = core.produces.plus(Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList()),
            responses = core.responses.plus(new.second.status to new)))

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    fun producing(vararg new: ContentType) = Desc(core.copy(produces = core.produces.plus(new)))
    fun consuming(vararg new: ContentType) = Desc(core.copy(consumes = core.consumes.plus(new)))

    companion object {
        internal data class Core(val summary: String,
                                 val description: String?,
                                 val body: BodyLens<*>?,
                                 val request: Request? = null,
                                 val tags: Set<Tag> = emptySet(),
                                 val produces: Set<ContentType> = emptySet(),
                                 val consumes: Set<ContentType> = emptySet(),
                                 val requestParams: List<Lens<Request, *>> = emptyList(),
                                 val responses: Map<Status, Pair<String, Response>> = emptyMap()) {

            internal val validationFilter = Filter {
                nextHandler ->
                {
                    val body = body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
                    val errors = body.plus(requestParams).fold(emptyList<Failure>()) { memo, next ->
                        try {
                            next(it)
                            memo
                        } catch (e: LensFailure) {
                            memo.plus(e.failures)
                        }
                    }
                    if (errors.isEmpty()) nextHandler(it) else throw LensFailure(errors)
                }
            }
        }
    }
}