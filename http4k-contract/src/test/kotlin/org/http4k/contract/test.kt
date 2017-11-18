package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.Lens
import org.http4k.lens.Query

class ContractDsl {
    var renderer: ContractRenderer = NoRenderer
    var security: Security = NoSecurity
    var descriptionPath = ""
    var summary: String = "<unknown>"
}

class Appendable<T>(val all: MutableList<T> = mutableListOf()) {
    operator fun plusAssign(t: T) {
        all += t
    }

    operator fun plusAssign(t: List<T>) {
        all += t
    }
}

class ContractRouteDsl {
    var summary: String = "<unknown>"
    var description: String? = null
    var request: Request? = null
    val tags = Appendable<Tag>()
    val produces = Appendable<ContentType>()
    val consumes = Appendable<ContentType>()
    internal val responses = Appendable<Pair<Status, Pair<String, Response>>>()
    var headers = Appendable<Lens<Request, *>>()
    var queries = Appendable<Lens<Request, *>>()
    var body: BodyLens<*>? = null

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) {
        produces += (Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList())
        responses += new.second.status to new
    }

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    @JvmName("returningStatus")
    fun returning(new: Status) = returning("" to Response(new))

    fun <T> receiving(new: Pair<BiDiBodyLens<T>, T>) {
        request = Request(Method.GET, "").with(new.first of new.second)
    }
}

fun contractRoute(fn: ContractRouteDsl.() -> Unit): Any = ContractRouteDsl().apply(fn).run {
    this
}

fun contractDsl(fn: ContractDsl.() -> Unit): Any = ContractDsl().apply(fn).run {
    this
}

val b = contractRoute {
    summary = "summary of this route"
    description = "some rambling description of what this thing actually does"
    produces += ContentType.APPLICATION_JSON
    tags += Tag("tag3")
    tags += Tag("tag1")
    headers += Header.optional("header", "description of the header")
    queries += Query.optional("header", "description of the header")
} //"/echo" / Path.of("message")

val a = contractDsl {
    renderer = OpenApi(ApiInfo("foo", "bbb", "asd"), Argo)
    security = ApiKey(Query.required("the_api_key"), { true })
//        routes += contractRoute"/echo" / Path.of("message")
//    header Header . optional ("header", "description of the header")
//    bindContract Method . GET to { msg -> { Response(Status.OK).body(msg) } }
//    meta meta {
//        summary = "summary of this route"
//        description = "some rambling description of what this thing actually does"
//        produces += ContentType.APPLICATION_JSON
//        tags += Tag("tag3")
//        tags += Tag("tag1")
//        returning("no way jose" to Status.FORBIDDEN)
//    },
//
//    "/simples" bindContract Method.GET to { Response(Status.OK) } meta RouteMeta("a simple endpoint")
//    )
}
