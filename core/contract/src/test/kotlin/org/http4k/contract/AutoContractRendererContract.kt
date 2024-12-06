package org.http4k.contract

import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Jackson.auto
import org.http4k.format.auto
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.MultipartForm
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Query
import org.http4k.lens.Validator.Strict
import org.http4k.lens.multipartForm
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
abstract class AutoContractRendererContract<NODE : Any>(
    override val json: AutoMarshallingJson<NODE>,
    rendererToUse: ContractRenderer
) : ContractRendererContract<NODE>(json, rendererToUse) {

    @Test
    open fun `auto rendering renders as expected`(approver: Approver) {
        val negotiator = ContentNegotiation.auto(
            Body.string(ContentType("custom/v1")).toLens(),
            Body.string(ContentType("custom/v2")).toLens()
        )

        val router = "/basepath" bind contract {
            renderer = rendererToUse
            tags += Tag("hello", "world")
            security = ApiKeySecurity(Query.required("the_api_key"), { true })
            routes += "/body_auto_schema" meta {
                receiving(
                    Body.auto<ArbObject2>().toLens() to ArbObject2(
                        "s",
                        ArbObject1(Foo.bar, listOf(Foo.bar)),
                        listOf(1),
                        true
                    ), "someOtherId"
                )
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_custom_request_content_type" meta {
                receiving(Body.auto<ArbObject4>(contentType = APPLICATION_YAML).toLens() to ArbObject4(Foo.bar))
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_custom_content_type" meta {
                returning(OK, Body.auto<ArbObject4>(contentType = APPLICATION_YAML).toLens() to ArbObject4(Foo.bar))
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema" meta {
                receiving(
                    Body.auto<ArbObject3>().toLens() to ArbObject3(
                        Uri.of("http://foowang"),
                        mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing, listOf(Foo.bing)))
                    )
                )
                returning(
                    status = CREATED,
                    body = Body.auto<List<ArbObject1>>().toLens() to listOf(ArbObject1(Foo.bing, listOf(Foo.bing)))
                )
            } bindContract PUT to { _ -> Response(OK) }
            routes += "/body_auto_schema_multiple_response_schemas" meta {
                returning(OK, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing, listOf(Foo.bing)))
                returning(CREATED, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing, listOf(Foo.bing)))
                returning(
                    CREATED,
                    Body.auto<ArbObject3>().toLens() to ArbObject3(
                        Uri.of("http://foowang"),
                        mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing, listOf(Foo.bing)))
                    )
                )
            } bindContract POST to { _ -> Response(OK) }
            routes += "/callback_with_body" meta {
                callback("name") {
                    "/foo" meta {
                        receiving(
                            Body.auto<ArbObject5>().toLens() to ArbObject5(
                                ArbObject3(
                                    Uri.of("http://foowang"),
                                    mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing, listOf(Foo.bing)))
                                )
                            )
                        )
                    } bindCallback POST
                }
                callback("name") {
                    "/bar" meta {
                        returning(
                            status = CREATED,
                            body = Body.auto<ArbObject6>().toLens() to ArbObject6(Foo.bing)
                        )
                    } bindCallback PUT
                }
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_multiple_request_schemas" meta {
                receiving(Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing, listOf(Foo.bing)))
                receiving(
                    Body.auto<ArbObject3>().toLens() to ArbObject3(
                        Uri.of("http://foowang"),
                        mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing, listOf(Foo.bing)))
                    )
                )
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_name_definition_id" meta {
                val toLens = Body.auto<InterfaceHolder>().toLens()
                returning(OK, toLens to InterfaceHolder(Impl1()), definitionId = "impl1")
                returning(OK, toLens to InterfaceHolder(Impl2()), definitionId = "impl2")
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_map" meta {
                receiving(
                    Body.auto<Map<String, *>>().toLens() to mapOf(
                        "foo" to 123,
                        "arb" to ArbObject1(Foo.bing, listOf(Foo.bing))
                    )
                )
            } bindContract PUT to { _ -> Response(OK) }
            routes += "/body_negotiated" meta {
                receiving(negotiator to "john")
                returning(OK, negotiator to "john")
            } bindContract POST to { _ -> Response(OK) }
            routes += "/multipart-fields" meta {
                val field = MultipartFormField.multi.required("stringField")
                val pic = MultipartFormFile.required("fileField")
                val autoJson = MultipartFormField.auto<ArbObject1>(json).required("autoJsonField")
                receiving(
                    Body.multipartForm(Strict, field, pic, autoJson).toLens() to MultipartForm()
                        .with(
                            field of listOf(MultipartFormField("hello")),
                            pic of MultipartFormFile("foo", APPLICATION_YAML, "hello".byteInputStream()),
                            autoJson of ArbObject1(Foo.bing, listOf(Foo.bing))
                        )
                )
            } bindContract PUT to { _ -> Response(OK) }
        }

        approver.assertApproved(router(Request(GET, "/basepath?the_api_key=somevalue")))
    }
}


