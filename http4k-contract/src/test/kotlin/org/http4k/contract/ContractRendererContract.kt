package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
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
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
abstract class ContractRendererContract(private val rendererToUse: ContractRenderer) {
    @Test
    fun `can build 400`() {
        val response = rendererToUse.badRequest(listOf(
            Missing(Meta(true, "location1", StringParam, "name1")),
            Invalid(Meta(false, "location2", NumberParam, "name2"))))
        assertThat(response.bodyString(),
            equalTo("""{"message":"Missing/invalid parameters","params":[{"name":"name1","type":"location1","datatype":"string","required":true,"reason":"Missing"},{"name":"name2","type":"location2","datatype":"number","required":false,"reason":"Invalid"}]}"""))
    }

    @Test
    fun `can build 404`() {
        val response = rendererToUse.notFound()
        assertThat(response.bodyString(),
            equalTo("""{"message":"No route found on this path. Have you used the correct HTTP verb?"}"""))
    }

    @Test
    open fun `renders as expected`(approver: Approver) {
        val customBody = Body.json("the body of the message").toLens()

        val router = "/basepath" bind contract {
            renderer = rendererToUse
            security = ApiKeySecurity(Query.required("the_api_key"), { true })
            routes += "/nometa" bindContract GET to { Response(OK) }
            routes += "/descriptions" meta {
                summary = "endpoint"
                description = "some rambling description of what this thing actually does"
                operationId = "echoMessage"
                tags += Tag("tag3")
                tags += Tag("tag1")
            } bindContract GET to { Response(OK) }
            routes += "/paths" / Path.of("firstName") / "bertrand" / Path.boolean().of("age") bindContract POST to { a, _, _ -> { Response(OK).body(a) } }
            routes += "/queries" meta {
                queries += Query.boolean().required("b", "booleanQuery")
                queries += Query.string().optional("s", "stringQuery")
                queries += Query.int().optional("i", "intQuery")
                queries += Query.json().optional("j", "jsonQuery")
            } bindContract POST to { Response(OK).body("hello") }
            routes += "/headers" meta {
                headers += Header.boolean().required("b", "booleanHeader")
                headers += Header.string().optional("s", "stringHeader")
                headers += Header.int().optional("i", "intHeader")
                headers += Header.json().optional("j", "jsonHeader")
            } bindContract POST to { Response(OK).body("hello") }
            routes += "/body_string" meta {
                receiving(Body.string(TEXT_PLAIN).toLens())
            } bindContract POST to { Response(OK) }
            routes += "/body_json_noschema" meta {
                receiving(Body.json("json").toLens())
            } bindContract POST to { Response(OK) }
            routes += "/body_json_schema" meta {
                receiving(Body.json("json").toLens() to Jackson {
                    obj("anAnotherObject" to obj("aNullField" to nullNode(), "aNumberField" to number(123)))
                }, "someDefinitionId")
            } bindContract POST to { Response(OK) }
            routes += "/security" meta {
                security = BasicAuthSecurity("realm", Credentials("user", "password"))
            } bindContract POST to { Response(OK) }
            routes += "/body_form" meta {
                receiving(Body.webForm(Validator.Strict,
                    FormField.boolean().required("b", "booleanField"),
                    FormField.int().optional("i", "intField"),
                    FormField.string().optional("s", "stringField"),
                    FormField.json().required("j", "jsonField")
                ).toLens())
            } bindContract POST to { Response(OK) }
            routes += "/produces_and_consumes" meta {
                produces += APPLICATION_JSON
                produces += APPLICATION_XML
                consumes += OCTET_STREAM
                consumes += APPLICATION_FORM_URLENCODED
            } bindContract GET to { Response(OK) }
            routes += "/returning" meta {
                returning("no way jose" to Response(FORBIDDEN).with(customBody of Jackson { obj("aString" to string("a message of some kind")) }))
            } bindContract POST to { Response(OK) }
            routes += "/no_security" meta {
                security = NoSecurity
            } bindContract POST to { Response(OK) }

            routes += specificRoutes()
        }

        approver.assertApproved(router(Request(GET, "/basepath?the_api_key=somevalue")))
    }

    open fun specificRoutes(): List<ContractRoute> = emptyList()
}
