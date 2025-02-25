package org.http4k.connect.amazon

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.AutoMarshalling

class AwsRestJsonFake(
    val autoMarshalling: AutoMarshalling,
    val awsService: AwsService,
    val region: Region,
    val accountId: AwsAccount
) {
    inline fun <reified RequestBody : Any> route(
        crossinline responseFn: (Any) -> Response = {
            Response(OK).body(autoMarshalling.asFormatString(it))
        },
        crossinline errorFn: (Any) -> Response = {
            Response(BAD_REQUEST).body(autoMarshalling.asFormatString(it))
                .also { println(it) }
        },
        crossinline fn: Request.(RequestBody) -> Result<Any, Any>,
    ): HttpHandler = { req ->
        val body = autoMarshalling.asA<RequestBody>(req.bodyString())
        fn(req, body)
            .map(responseFn)
            .recover { err ->
                when(err) {
                    is RestfulError -> {
                        val message = """{"message":"${err.message}","resourceId":${err.resourceId?.let { "\"$it\"" } ?: "null"},"resourceType":${err.resourceType?.let { "\"$it\"" } ?: "null"}}"""
                        Response(err.status).body(message)
                    }
                    else -> errorFn(err)
                }
            }
    }
}

data class RestfulError(val status: Status, val message: String, val resourceId: ARN?, val resourceType: String?)
