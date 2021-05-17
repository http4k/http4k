package guide.howto.multipart_forms

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
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
import org.http4k.lens.multipartForm
import org.http4k.server.SunHttp
import org.http4k.server.asServer

data class Name(val value: String)

fun main() {
    // define fields using the standard lens syntax
    val nameField = MultipartFormField.string().map(::Name, Name::value).required("name")
    val imageFile = MultipartFormFile.optional("image")

    // add fields to a form definition, along with a validator
    val strictFormBody = Body.multipartForm(Validator.Strict, nameField, imageFile, diskThreshold = 5).toLens()

    val server = ServerFilters.CatchAll().then { r: Request ->

        // to extract the contents, we first extract the form and then extract the fields from it using the lenses
        // NOTE: we are "using" the form body here because we want to close the underlying file streams
        strictFormBody(r).use {
            println(nameField(it))
            println(imageFile(it))
        }

        Response(OK)
    }.asServer(SunHttp(8000)).start()

    // creating valid form using "with()" and setting it onto the request. The content type and boundary are
    // taken care of automatically
    val multipartform = MultipartForm().with(
        nameField of Name("rita"),
        imageFile of MultipartFormFile("image.txt", ContentType.OCTET_STREAM, "somebinarycontent".byteInputStream()))
    val validRequest = Request(POST, "http://localhost:8000").with(strictFormBody of multipartform)

    println(ApacheClient()(validRequest))

    server.stop()
}
