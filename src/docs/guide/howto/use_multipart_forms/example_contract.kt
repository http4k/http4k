package guide.howto.use_multipart_forms


import org.http4k.contract.PreFlightExtraction
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Validator.Strict
import org.http4k.lens.instant
import org.http4k.lens.multipartForm
import org.http4k.server.ApacheServer
import org.http4k.server.asServer


fun main() {
    val documentPart = MultipartFormFile.required("document")
    val ownerPart = MultipartFormField.string().required("owner")
    val signaturePart = MultipartFormField.string().instant().required("signedAt")

    val formLens = Body.multipartForm(Strict, documentPart, ownerPart, signaturePart).toLens()

    val handler = contract {
        renderer = OpenApi3(ApiInfo("My great API", "v1.0"), Jackson)
        descriptionPath = "/openapi.json"

        routes += "/api/document-upload" meta {
            summary = "Uploads a document including the owner name and when it was signed"

            // required to avoid reading the multipart stream twice!
            preFlightExtraction = PreFlightExtraction.IgnoreBody

            receiving(formLens)
            returning(OK)
        } bindContract POST to { req ->
            formLens(req).use {
                val doc = documentPart(it)
                val owner = ownerPart(it)
                val signatureDate = signaturePart(it)
                //process file...
                Response(OK).body("${doc.filename} by $owner, signed at $signatureDate")
            }
        }
    }

    /**
     * example request:
     * curl -v -H 'Content-Type: multipart/form-data' \
     *      -F owner="John Doe" \
     *      -F signedAt="2011-12-03T10:15:30Z" \
     *      -F document=@README.md \
     *      http://localhost:8081/api/document-upload
     */

    handler.asServer(ApacheServer(8081)).start()
}

