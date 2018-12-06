package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
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
import org.http4k.format.Argo.prettify
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.Invalid
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.Validator
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

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
            "/echo" / Path.of("message") meta {
                summary = "summary of this route"
                description = "some rambling description of what this thing actually does"
                operationId = "echoMessage"
                headers += Header.optional("header", "description of the header")
                produces += APPLICATION_JSON
                returning("peachy" to Response(OK).with(customBody of Argo.obj("anAnotherObject" to Argo.obj("aNumberField" to Argo.number(123)))))
                returning(ResponseMeta("peachy",
                    Response(ACCEPTED).with(customBody of Argo.obj("anAnotherObject" to Argo.obj("aNumberField" to Argo.number(123)))), "someDefinitionId"))
                returning("no way jose" to FORBIDDEN)
                tags += Tag("tag3")
                tags += Tag("tag1")
            } bindContract GET to { msg -> { Response(OK).body(msg) } },
            "/echo" / Path.of("message") meta {
                summary = "a post endpoint"
                queries += Query.int().required("query")
                consumes += listOf(ContentType.APPLICATION_XML, APPLICATION_JSON)
                produces += APPLICATION_JSON
                returning("no way jose" to Response(FORBIDDEN).with(customBody of Argo.obj("aString" to Argo.string("a message of some kind"))))
                tags += Tag("tag1")
                tags += listOf(Tag("tag2", "description of tag"), Tag("tag2", "description of tag"))
                receiving(customBody to Argo.obj("anObject" to Argo.obj("notAStringField" to Argo.number(123))), "someOtherDefinitionId")
            } bindContract POST to { msg -> { Response(OK).body(msg) } },
            "/welcome" / Path.of("firstName") / "bertrand" / Path.of("secondName") meta {
                summary = "a friendly endpoint"
                queries += Query.boolean().required("query", "description of the query")
                receiving(Body.webForm(Validator.Strict,
                    FormField.int().required("intForm", "description of the form field"),
                    FormField.json().required("jsonForm", "description of the json form field")
                ).toLens())
            } bindContract GET to { a, _, _ -> { Response(OK).body(a) } },
            "/noexamplejson" meta { receiving(Body.json("json").toLens()) } bindContract GET to { Response(OK) },
            "/noexample" meta { receiving(Body.string(TEXT_PLAIN).toLens()) } bindContract GET to { Response(OK) },
            "/simples" meta { summary = "a simple endpoint" } bindContract GET to { Response(OK) }
        )

        val expected = String(this.javaClass.getResourceAsStream("${this.javaClass.simpleName}.json").readBytes())
        val actual = router(Request(Method.GET, "/basepath?the_api_key=somevalue")).bodyString()
        println(actual)
        assertThat(prettify(actual), equalTo(prettify(expected)))
    }
}
