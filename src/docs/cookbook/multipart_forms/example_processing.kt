package cookbook.multipart_forms

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.MultipartEntity
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ProcessFiles
import org.http4k.filter.ServerFilters
import org.http4k.lens.FormField
import org.http4k.lens.MultipartForm
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Validator
import org.http4k.lens.multipartForm
import org.http4k.lens.webForm
import org.http4k.server.SunHttp
import org.http4k.server.asServer

data class AName(val value: String)

fun main() {

    val server = ServerFilters.ProcessFiles { multipartFile: MultipartEntity.File ->
        // do something with the file right here... like stream it to another server and return the reference
        println(String(multipartFile.file.content.readBytes()))
        multipartFile.file.filename
    }
        .then { req: Request ->
            // this is the web-form definition - it it DIFFERENT to the multipart form definition,
            // because the fields and content-type have been replaced in the ProcessFiles filter
            val nameField = FormField.map(::AName, AName::value).required("name")
            val imageFile = FormField.optional("image")
            val body = Body.webForm(Validator.Strict, nameField, imageFile).toLens()

            println(body(req))

            Response(Status.OK)
        }.asServer(SunHttp(8000)).start()

    println(ApacheClient()(buildValidMultipartRequest()))

    server.stop()
}

private fun buildValidMultipartRequest(): Request {
    // define fields using the standard lens syntax
    val nameField = MultipartFormField.string().map(::AName, AName::value).required("name")
    val imageFile = MultipartFormFile.optional("image")

    // add fields to a form definition, along with a validator
    val strictFormBody = Body.multipartForm(Validator.Strict, nameField, imageFile, diskThreshold = 5).toLens()

    val multipartform = MultipartForm().with(
        nameField of AName("rita"),
        imageFile of MultipartFormFile("image.txt", ContentType.OCTET_STREAM, "somebinarycontent".byteInputStream()))
    return Request(POST, "http://localhost:8000").with(strictFormBody of multipartform)
}
