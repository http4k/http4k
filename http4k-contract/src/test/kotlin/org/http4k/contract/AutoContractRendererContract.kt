package org.http4k.contract

import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Jackson.auto
import org.http4k.format.Negotiator
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
abstract class AutoContractRendererContract<NODE : Any>(
    json: AutoMarshallingJson<NODE>,
    rendererToUse: ContractRenderer
) : ContractRendererContract<NODE>(json, rendererToUse) {

    @Test
    open fun `auto rendering renders as expected`(approver: Approver) {
        val negotiator = ContentNegotiation.Negotiator(
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
                        ArbObject1(Foo.bar),
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
                receiving(Body.auto<ArbObject3>().toLens() to ArbObject3(Uri.of("http://foowang"), mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing))))
                returning(
                    status = Status.CREATED,
                    body = Body.auto<List<ArbObject1>>().toLens() to listOf(ArbObject1(Foo.bing))
                )
            } bindContract Method.PUT to { _ -> Response(OK) }
            routes += "/body_auto_schema_multiple_response_schemas" meta {
                returning(OK, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
                returning(Status.CREATED, Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
                returning(
                    Status.CREATED,
                    Body.auto<ArbObject3>().toLens() to ArbObject3(Uri.of("http://foowang"), mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing)))
                )
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_multiple_request_schemas" meta {
                receiving(Body.auto<ArbObject1>().toLens() to ArbObject1(Foo.bing))
                receiving(Body.auto<ArbObject3>().toLens() to ArbObject3(Uri.of("http://foowang"), mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing))))
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_schema_name_definition_id" meta {
                val toLens = Body.auto<InterfaceHolder>().toLens()
                returning(OK, toLens to InterfaceHolder(Impl1()), definitionId = "impl1")
                returning(OK, toLens to InterfaceHolder(Impl2()), definitionId = "impl2")
            } bindContract POST to { _ -> Response(OK) }
            routes += "/body_auto_map" meta {
                receiving(Body.auto<Map<String, *>>().toLens() to mapOf("foo" to 123, "arb" to ArbObject1(Foo.bing)))
            } bindContract Method.PUT to { _ -> Response(OK) }
            routes += "/body_negotiated" meta {
                receiving(negotiator to "john")
                returning(OK, negotiator to "john")
            } bindContract POST to { _ -> Response(OK) }
        }

        approver.assertApproved(router(Request(Method.GET, "/basepath?the_api_key=somevalue")))
    }
}
