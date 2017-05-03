package org.reekwest.http.contract.module

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Method
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.lens.Header
import org.reekwest.http.lens.Invalid
import org.reekwest.http.lens.Meta
import org.reekwest.http.lens.Missing
import org.reekwest.http.lens.Path
import org.reekwest.http.lens.Query

abstract class ModuleRendererContract(private val renderer: ModuleRenderer) {
    fun name(): String = this.javaClass.simpleName

    @Test
    fun `can build 400`() {
        val response = renderer.badRequest(listOf(
            Missing(Meta(true, "location1", "name1")),
            Invalid(Meta(false, "location2", "name2"))))
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

//        val customBody = Body.json().required("the body of the message", json.obj("anObject" to json.obj("notAStringField" to json.number(123))))

        val module = RouteModule(Root / "basepath", renderer)
            .securedBy(ApiKey(Query.required("the_api_key"), { true }))
            .withRoute(
                Route("summary of this route", "some rambling description of what this thing actually does")
                    .producing(ContentType.APPLICATION_JSON)
                    .header(Header.optional("header", "description of the header"))
//                    .returning(ResponseSpec.json(Status.Ok to "peachy", obj("anAnotherObject" to obj("aNumberField" to number(123)))))
//        .returning(Status.Forbidden to "no way jose")
                    .at(Method.GET) / "echo" / Path.of("message") bind { msg -> { Response(Status.OK).body(msg) } })
//        .withRoute(
//            RouteSpec("a post endpoint")
//                .consuming(APPLICATION_ATOM_XML, APPLICATION_SVG_XML)
//                .producing(APPLICATION_JSON)
//                .returning(ResponseSpec.json(Status.Forbidden to "no way jose", obj("aString" to Argo.JsonFormat.string("a message of some kind"))))
//        .taking(Query.required.int("query"))
//            .body(customBody)
//            .at(Post) / "echo" / Path.string("message") bindTo ((s: String) -> Echo(s)))
//        .withRoute(
//            RouteSpec("a friendly endpoint")
//                .taking(Query.required.boolean("query", "description of the query"))
//                .body(Body.form(FormField.required.int("form", "description of the form")))
//                .at(Get) / "welcome" / Path.string("firstName") / "bertrand" / Path.string("secondName") bindTo ((x: String, y: String, z: String) -> Echo(x, y, z)))

//                    val expected = parse(Source.fromInputStream(this.getClass.getResourceAsStream("$name.json")).mkString)
//
//                    val actual = Await.result(module.toHtt(Request("/basepath"))).contentString
//                    //                  println(actual)
//                    parse(actual) shouldBe expected
    }
}