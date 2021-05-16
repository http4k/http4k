package howto.multipart_forms

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.MultipartEntity
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.multipartIterator
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

fun main() {

    val server = ServerFilters.CatchAll().then { r: Request ->

        // here we are iterating over the multiparts as we read them out of the input
        val fields = r.multipartIterator().asSequence().fold(emptyList<MultipartEntity.Field>()) { memo, next ->
            when (next) {
                is MultipartEntity.File -> {
                    // do something with the file right here... like stream it to another server
                    memo
                }
                is MultipartEntity.Field -> memo.plus(next)
            }
        }

        println(fields)

        Response(OK)
    }.asServer(SunHttp(8000)).start()

    println(ApacheClient()(buildMultipartRequest()))

    server.stop()
}

private fun buildMultipartRequest(): Request {
    // define fields using the standard lens syntax
    val nameField = MultipartFormField.string().map(::Name, Name::value).required("name")
    val imageFile = MultipartFormFile.optional("image")

    // add fields to a form definition, along with a validator
    val strictFormBody = Body.multipartForm(Validator.Strict, nameField, imageFile, diskThreshold = 5).toLens()

    val multipartform = MultipartForm().with(
        nameField of Name("rita"),
        imageFile of MultipartFormFile("image.txt", ContentType.OCTET_STREAM, "somebinarycontent".byteInputStream()))
    return Request(POST, "http://localhost:8000").with(strictFormBody of multipartform)
}
