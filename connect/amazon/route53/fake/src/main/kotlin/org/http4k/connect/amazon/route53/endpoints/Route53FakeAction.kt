package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType
import org.w3c.dom.Document

fun <RequestBody: Any, ResponseBody: Any> route53FakeAction(
    requestBodyFn: ((Document) -> RequestBody),
    successFn: (ResponseBody) -> String,
    errorFn: (Route53Error) -> Response = { Response(it.status).body(it.message) },
    successCode: Status = Status.OK,
    fn: Request.(RequestBody) -> Result4k<ResponseBody, Route53Error>
): HttpHandler = { request ->
    request.body.xmlDoc()
        .let(requestBodyFn)
        .let { fn(request, it) }
        .map {
            Response(successCode)
                .contentType(ContentType.APPLICATION_XML)
                .body("""<?xml version="1.0" encoding="UTF-8"?>${successFn(it)}""")
        }
        .recover(errorFn)
}

fun <ResponseBody: Any> route53FakeAction(
    successFn: (ResponseBody) -> String,
    errorFn: (Route53Error) -> Response = { Response(it.status).body(it.message) },
    successCode: Status = Status.OK,
    fn: Request.() -> Result4k<ResponseBody, Route53Error>
): HttpHandler = { request ->
    fn(request)
        .map {
            Response(successCode)
                .contentType(ContentType.APPLICATION_XML)
                .body("""<?xml version="1.0" encoding="UTF-8"?>${successFn(it)}""")
        }
        .recover(errorFn)
}
