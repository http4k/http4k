package org.http4k.connect.amazon.route53.endpoints

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.route53.model.FakeRoute53Error
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.contentType
import org.w3c.dom.Document

fun <RequestBody: Any, ResponseBody: Any> route53FakeAction(
    requestBodyFn: ((Document) -> RequestBody),
    responseBodyFn: (ResponseBody) -> String,
    successCode: Status = Status.OK,
    fn: Request.(RequestBody) -> Result4k<ResponseBody, FakeRoute53Error>
): HttpHandler = { request ->
    request.body.xmlDoc()
        .let(requestBodyFn)
        .let { fn(request, it) }
        .map {
            Response(successCode)
                .contentType(ContentType.APPLICATION_XML)
                .body("""<?xml version="1.0" encoding="UTF-8"?>${responseBodyFn(it)}""")
        }
        .recover { it.toResponse() }
}

fun <ResponseBody: Any> route53FakeAction(
    responseBodyFn: (ResponseBody) -> String,
    successCode: Status = Status.OK,
    fn: Request.() -> Result4k<ResponseBody, FakeRoute53Error>
): HttpHandler = { request ->
    fn(request)
        .map {
            Response(successCode)
                .contentType(ContentType.APPLICATION_XML)
                .body("""<?xml version="1.0" encoding="UTF-8"?>${responseBodyFn(it)}""")
        }
        .recover { it.toResponse() }
}
