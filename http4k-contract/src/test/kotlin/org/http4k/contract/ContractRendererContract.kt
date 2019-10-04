package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.AuthCodeOAuthSecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.contract.security.and
import org.http4k.contract.security.or
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.format.Json
import org.http4k.lens.Cookies
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.Invalid
import org.http4k.lens.LensFailure
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
import org.http4k.security.FakeOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.gitHub
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
abstract class ContractRendererContract<NODE>(private val json: Json<NODE>, protected val rendererToUse: ContractRenderer) {
    @Test
    fun `can build 400`() {
        val response = rendererToUse.badRequest(LensFailure(listOf(
            Missing(Meta(true, "location1", StringParam, "name1")),
            Invalid(Meta(false, "location2", NumberParam, "name2")))))
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
        val customBody = json.body("the body of the message").toLens()

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
                queries += json.lens(Query).optional("j", "jsonQuery")
            } bindContract POST to { Response(OK).body("hello") }
            routes += "/cookies" meta {
                cookies += Cookies.required("b", "requiredCookie")
                cookies += Cookies.optional("s", "optionalCookie")
            } bindContract POST to { Response(OK).body("hello") }
            routes += "/headers" meta {
                headers += Header.boolean().required("b", "booleanHeader")
                headers += Header.string().optional("s", "stringHeader")
                headers += Header.int().optional("i", "intHeader")
                headers += json.lens(Header).optional("j", "jsonHeader")
            } bindContract POST to { Response(OK).body("hello") }
            routes += "/body_string" meta {
                receiving(Body.string(TEXT_PLAIN).toLens())
            } bindContract POST to { Response(OK) }
            routes += "/body_json_noschema" meta {
                receiving(json.body("json").toLens())
            } bindContract POST to { Response(OK) }
            routes += "/body_json_response" meta {
                returning("normal" to json {
                    val obj = obj("aNullField" to nullNode(), "aNumberField" to number(123))
                    Response(OK).with(body("json").toLens() of obj)
                })
            } bindContract POST to { Response(OK) }
            routes += "/body_json_schema" meta {
                receiving(json.body("json").toLens() to json {
                    obj("anAnotherObject" to obj("aNullField" to nullNode(), "aNumberField" to number(123)))
                }, "someDefinitionId")
            } bindContract POST to { Response(OK) }
            routes += "/body_json_list_schema" meta {
                receiving(json.body("json").toLens() to json {
                    array(obj("aNumberField" to number(123)))
                })
            } bindContract POST to { Response(OK) }
            routes += "/basic_auth" meta {
                security = BasicAuthSecurity("realm", credentials)
            } bindContract POST to { Response(OK) }
            routes += "/and_auth" meta {
                security = BasicAuthSecurity("foo", credentials, "and1").and(BasicAuthSecurity("foo", credentials, "and2"))
            } bindContract POST to { Response(OK) }
            routes += "/or_auth" meta {
                security = BasicAuthSecurity("foo", credentials, "or1").or(BasicAuthSecurity("foo", credentials, "or2"))
            } bindContract POST to { Response(OK) }
            routes += "/oauth2_auth" meta {
                security = AuthCodeOAuthSecurity(OAuthProvider.gitHub({ Response(OK) },
                    credentials,
                    Uri.of("http://localhost/callback"),
                    FakeOAuthPersistence()))
            } bindContract POST to { Response(OK) }
            routes += "/body_form" meta {
                receiving(Body.webForm(Validator.Strict,
                    FormField.boolean().required("b", "booleanField"),
                    FormField.int().optional("i", "intField"),
                    FormField.string().optional("s", "stringField"),
                    json.lens(FormField).required("j", "jsonField")
                ).toLens())
            } bindContract POST to { Response(OK) }
            routes += "/produces_and_consumes" meta {
                produces += APPLICATION_JSON
                produces += APPLICATION_XML
                consumes += OCTET_STREAM
                consumes += APPLICATION_FORM_URLENCODED
            } bindContract GET to { Response(OK) }
            routes += "/returning" meta {
                returning("no way jose" to Response(FORBIDDEN).with(customBody of json { obj("aString" to string("a message of some kind")) }))
            } bindContract POST to { Response(OK) }
            routes += "/body_auto_schema" meta {
                receiving(Body.auto<ArbObject2>().toLens() to ArbObject2(
                    "s",
                    ArbObject1(Foo.bar),
                    listOf(1),
                    true
                ), "someOtherId")
            } bindContract POST to { Response(OK) }
            routes += "/body_auto_schema" meta {
                receiving(Body.auto<ArbObject3>().toLens() to ArbObject3(Uri.of("http://foowang"), mapOf("foo" to 123)))
                returning(status = SEE_OTHER, body = Body.auto<List<ArbObject1>>().toLens() to listOf(ArbObject1(Foo.bing)))
            } bindContract PUT to { Response(OK) }
            routes += "/body_auto_schema_multiple_body_schemas" meta {
                returning(OK, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
                returning(SEE_OTHER, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
                returning(SEE_OTHER, Body.auto<ArbObject3>().toLens() to ArbObject3(Uri.of("http://foowang"), mapOf("foo" to 123)))
            } bindContract POST to { Response(OK) }
            routes += "/body_auto_map" meta {
                receiving(Body.auto<Map<String, *>>().toLens() to mapOf("foo" to 123))
            } bindContract PUT to { Response(OK) }
            routes += "/bearer_auth" meta {
                security = BearerAuthSecurity("foo")
            } bindContract POST to { Response(OK) }
        }

        approver.assertApproved(router(Request(GET, "/basepath?the_api_key=somevalue")))
    }

    @Test
    fun `when enabled renders description including its own path`(approver: Approver) {
        val router = "/" bind contract {
            renderer = rendererToUse
            security = ApiKeySecurity(Query.required("the_api_key"), { true })
            routes += "/" bindContract GET to { Response(OK) }
            descriptionPath = "/docs"
            includeDescriptionRoute = true
        }

        approver.assertApproved(router(Request(GET, "/docs?the_api_key=somevalue")))
    }

}

private val credentials = Credentials("user", "password")

enum class Foo {
    bar, bing
}

data class ArbObject1(val anotherString: Foo)
data class ArbObject2(val string: String, val child: ArbObject1?, val numbers: List<Int>, val bool: Boolean)
data class ArbObject3(val uri: Uri, val additional: Map<String, *>)
