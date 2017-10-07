import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.MultipartForm
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Validator
import org.http4k.lens.int
import org.http4k.lens.multipartForm
import org.http4k.server.SunHttp
import org.http4k.server.asServer


fun main(args: Array<String>) {

    val field = MultipartFormField.int().required("bob")
    val file = MultipartFormFile.required("bill")
    val form = Body.multipartForm(Validator.Strict, field, file).toLens()

    val s = ServerFilters.CatchAll().then({ r: Request ->

        val theForm = form.extract(r)
        println("I got a field " + field.extract(theForm))
        println("I got a file " + file.extract(theForm).content.reader().readText())

        Response(OK).body(r.toString())
    }).asServer(SunHttp(8000)).start()


    val req = Request(Method.POST, "http://localhost:8000/bob")
//    val req = Request(Method.POST, "http://httpbin.org/post")
        .with(form of MultipartForm().with(
            field of 123,
            file of MultipartFormFile("foo.txt", ContentType.TEXT_HTML, "some html".byteInputStream())))

    println(ApacheClient()(req))

    s.stop()
}