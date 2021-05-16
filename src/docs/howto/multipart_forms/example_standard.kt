package howto.multipart_forms

import org.http4k.client.ApacheClient
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {

    // extract the body from the request and then the fields/files from it
    val server = { r: Request ->
        val receivedForm = MultipartFormBody.from(r)
        println(receivedForm.fieldValues("field"))
        println(receivedForm.field("field2"))
        println(receivedForm.files("file"))

        Response(OK)
    }.asServer(SunHttp(8000)).start()

    // add fields and files to the multipart form body
    val body = MultipartFormBody()
        .plus("field" to "my-value")
        .plus("field2" to MultipartFormField("my-value2", listOf("my-header" to "my-value")))
        .plus("file" to MultipartFormFile("image.txt", ContentType.OCTET_STREAM, "somebinarycontent".byteInputStream()))

    // we need to set both the body AND the correct content type header on the the request
    val request = Request(POST, "http://localhost:8000")
        .header("content-type", "multipart/form-data; boundary=${body.boundary}")
        .body(body)

    println(ApacheClient()(request))

    server.stop()
}
