package org.reekwest.http.contract.module

import argo.jdom.JsonRootNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.ContentType.Companion.APPLICATION_JSON
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Method.POST
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.formats.Argo.json
import org.reekwest.http.formats.Argo.parse
import org.reekwest.http.lens.BiDiBodyLens
import org.reekwest.http.lens.Body
import org.reekwest.http.lens.FormField
import org.reekwest.http.lens.FormValidator.Strict
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Invalid
import org.reekwest.http.lens.Meta
import org.reekwest.http.lens.Missing
import org.reekwest.http.lens.ParamMeta.StringParam
import org.reekwest.http.lens.Path
import org.reekwest.http.lens.Query
import org.reekwest.http.lens.boolean
import org.reekwest.http.lens.int
import org.reekwest.http.lens.webForm

abstract class ModuleRendererContract(private val renderer: ModuleRenderer) {
    fun name(): String = this.javaClass.simpleName

    @Test
    fun `can build 400`() {
        val response = renderer.badRequest(listOf(
            Missing(Meta(true, "location1", StringParam, "name1")),
            Invalid(Meta(false, "location2", StringParam, "name2"))))
        assertThat(response.bodyString(),
            equalTo("""{"message":"Missing/invalid parameters","params":[{"name":"name1","type":"location1","required":true,"reason":"Missing"},{"name":"name2","type":"location2","required":false,"reason":"Invalid"}]}"""))
    }

    @Test
    fun `can build 404`() {
        val response = renderer.notFound()
        assertThat(response.bodyString(),
            equalTo("""{"message":"No route found on this path. Have you used the correct HTTP verb?"}"""))
    }


    @Test
    fun `renders as expected`() {

        val customBody: BiDiBodyLens<JsonRootNode> = Body.json().required("the body of the message")
//        , Argo.obj("anObject" to Argo.obj("notAStringField" to Argo.number(123))))

        val module = RouteModule(Root / "basepath", renderer)
            .securedBy(ApiKey(Query.required("the_api_key"), { true }))
            .withRoute(
                Route("summary of this route", "some rambling description of what this thing actually does")
                    .producing(APPLICATION_JSON)
                    .header(Header.optional("header", "description of the header"))
//                    .returning(ResponseSpec.json(Status.Ok to "peachy", obj("anAnotherObject" to obj("aNumberField" to number(123)))))
//        .returning(Status.Forbidden to "no way jose")
                    .at(GET) / "echo" / Path.of("message") bind { msg -> { Response(OK).body(msg) } })
            .withRoute(
                Route("a post endpoint")
                    .consuming(ContentType.APPLICATION_XML, APPLICATION_JSON)
                    .producing(APPLICATION_JSON)
//                .returning(ResponseSpec.json(Status.Forbidden to "no way jose", obj("aString" to Argo.JsonFormat.string("a message of some kind"))))
                    .query(Query.int().required("query"))
                    .body(customBody)
                    .at(POST) / "echo" / Path.of("message") bind { msg -> { Response(OK).body(msg) } })
            .withRoute(
                Route("a friendly endpoint")
                    .query(Query.boolean().required("query", "description of the query"))
                    .body(Body.webForm(Strict, FormField.int().required("form", "description of the form")))
                    .at(GET) / "welcome" / Path.of("firstName") / "bertrand" / Path.of("secondName") bind { a, b, c -> { Response(OK).body(a) } })

        val expected = String(this.javaClass.getResourceAsStream("${this.javaClass.simpleName}.json").readBytes())
        val actual = module.toHttpHandler()(get("/basepath?the_api_key=somevalue")).bodyString()
        println(expected)
        println(actual)
        assertThat(parse(actual), equalTo(parse(expected)))
    }
}