package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.format.Jackson.prettify
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

        val router = "/basepath" bind contract(renderer, "/", ApiKey(Query.required("the_api_key"), { true }),
            "/nometa" bindContract GET to { Response(OK) },
            "/descriptions" meta {
                summary = "endpoint"
                description = "some rambling description of what this thing actually does"
                operationId = "echoMessage"
                tags += Tag("tag3")
                tags += Tag("tag1")
            } bindContract GET to { Response(OK) },
            "/paths" / Path.of("firstName") / "bertrand" / Path.boolean().of("age") bindContract POST to { a, _, _ -> { Response(OK).body(a) } },
            "/queries" meta {
                queries += Query.boolean().required("b", "booleanQuery")
                queries += Query.string().optional("s", "stringQuery")
                queries += Query.int().optional("i", "intQuery")
                queries += Query.json().optional("j", "jsonQuery")
            } bindContract POST to { Response(OK).body("hello") },
            "/headers" meta {
                headers += Header.boolean().required("b", "booleanHeader")
                headers += Header.string().optional("s", "stringHeader")
                headers += Header.int().optional("i", "intHeader")
                headers += Header.json().optional("j", "jsonHeader")
            } bindContract POST to { Response(OK).body("hello") },
            "/body_string" meta { receiving(Body.string(ContentType.TEXT_PLAIN).toLens()) } bindContract POST to { Response(OK) },
            "/body_json_noschema" meta {
                receiving(Body.json("json").toLens())
            }
                bindContract POST to { Response(OK) },
            "/body_json_schema" meta {
                receiving(Body.json("json").toLens() to Jackson {
                    obj("anAnotherObject" to obj("aNullField" to nullNode(), "aNumberField" to number(123))) }, "someDefinitionId")
            }
                bindContract POST to { Response(OK) },
            "/body_form" meta {
                receiving(Body.webForm(Validator.Strict,
                    FormField.boolean().required("b", "booleanField"),
                    FormField.int().optional("i", "intField"),
                    FormField.string().optional("s", "stringField"),
                    FormField.json().required("j", "jsonField")
                ).toLens())
            } bindContract POST to { Response(OK) },
//            "/body_xml" meta { receiving(Body.xml("json").toLens() to Argo { obj("anAnotherObject" to obj("aNumberField" to number(123))) }) } bindContract GET to { Response(OK) },
            "/produces_and_consumes" meta {
                produces += APPLICATION_JSON
                produces += APPLICATION_XML
                consumes += OCTET_STREAM
                consumes += APPLICATION_FORM_URLENCODED
            } bindContract GET to { Response(OK) },
            "/returning" meta {
                returning("no way jose" to Response(FORBIDDEN).with(customBody of Jackson { obj("aString" to string("a message of some kind")) }))
            } bindContract POST to { Response(OK) }
        )

        val expected = String(javaClass.getResourceAsStream("${javaClass.simpleName}.json").readBytes())
        val actual = router(Request(Method.GET, "/basepath?the_api_key=somevalue")).bodyString()
//        ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive).then(router).asServer(SunHttp(8000)).start().block()
//        println(actual)
        assertThat("no match", prettify(actual), equalTo(prettify(expected)))
    }
}
