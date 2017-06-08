package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Argo
import org.http4k.format.Argo.json
import org.http4k.format.Argo.parse
import org.http4k.lens.FormField
import org.http4k.lens.FormValidator.Strict
import org.http4k.lens.Header
import org.http4k.lens.Invalid
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.webForm
import org.junit.Test

abstract class ContractRendererContract(private val renderer: ContractRenderer) {
    fun name(): String = this.javaClass.simpleName

    @Test
    fun `can build 400`() {
        val response = renderer.badRequest(listOf(
            Missing(Meta(true, "location1", StringParam, "name1")),
            Invalid(Meta(false, "location2", NumberParam, "name2"))))
        assertThat(response.bodyString(),
            equalTo("""{"message":"Missing/invalid parameters","params":[{"name":"name1","type":"location1","datatype":"string","required":true,"reason":"Missing"},{"name":"name2","type":"location2","datatype":"number","required":false,"reason":"Invalid"}]}"""))
    }

    @Test
    fun `can build 404`() {
        val response = renderer.notFound()
        assertThat(response.bodyString(),
            equalTo("""{"message":"No route found on this path. Have you used the correct HTTP verb?"}"""))
    }

    @Test
    fun `renders as expected`() {
        val customBody = Body.json("the body of the message").toLens()

        val router = "/basepath" bind contract(renderer, "", ApiKey(Query.required("the_api_key"), { true }),
            "/echo" / Path.of("message")
                header Header.optional("header", "description of the header")
                to GET
                bind { msg -> { Response(OK).body(msg) } }
                meta RouteMeta("summary of this route", "some rambling description of what this thing actually does")
                .producing(APPLICATION_JSON)
                .returning("peachy" to Response(OK).with(customBody of Argo.obj("anAnotherObject" to Argo.obj("aNumberField" to Argo.number(123)))))
                .returning("peachy" to Response(ACCEPTED).with(customBody of Argo.obj("anAnotherObject" to Argo.obj("aNumberField" to Argo.number(123)))))
                .returning("no way jose" to FORBIDDEN)
                .taggedWith("tag3")
                .taggedWith("tag1"),

            "/echo" / Path.of("message")
                query Query.int().required("query")
                body customBody
                to POST
                bind { msg -> { Response(OK).body(msg) } }
                meta RouteMeta("a post endpoint")
                .consuming(ContentType.APPLICATION_XML, APPLICATION_JSON)
                .producing(APPLICATION_JSON)
                .returning("no way jose" to Response(FORBIDDEN).with(customBody of Argo.obj("aString" to Argo.string("a message of some kind"))))
                .taggedWith("tag1")
                .taggedWith(Tag("tag2", "description of tag"), Tag("tag2", "description of tag"))
                .receiving(customBody to Argo.obj("anObject" to Argo.obj("notAStringField" to Argo.number(123)))),

            "/welcome" / Path.of("firstName") / "bertrand" / Path.of("secondName")
                query Query.boolean().required("query", "description of the query")
                body Body.webForm(Strict, FormField.int().required("form", "description of the form")).toLens()
                to GET
                bind { a, _, _ -> { Response(OK).body(a) } }
                meta RouteMeta("a friendly endpoint"),

            "/simples" to GET bind { Response(OK) } meta RouteMeta("a simple endpoint")
        )

        val expected = String(this.javaClass.getResourceAsStream("${this.javaClass.simpleName}.json").readBytes())
        val actual = router(Request(Method.GET, "/basepath?the_api_key=somevalue")).bodyString()
//        println(actual)
        assertThat(parse(actual), equalTo(parse(expected)))
    }
}