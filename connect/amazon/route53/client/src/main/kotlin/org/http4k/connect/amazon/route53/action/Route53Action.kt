package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.contentType
import org.w3c.dom.Document

abstract class Route53Action<R>(
    private val method: Method,
    private val uri: String,
    private val successFn: (Document) -> R
) : Action<Result4k<R, RemoteFailure>> {
    protected abstract fun toXml(): String

    override fun toRequest() = Request(method, uri)
        .contentType(ContentType.APPLICATION_XML)
        .body(toXml())

    override fun toResult(response: Response) = when {
        response.status.successful -> response.xmlDoc().let(successFn).asSuccess()
        else -> asRemoteFailure(response).asFailure()
    }
}
