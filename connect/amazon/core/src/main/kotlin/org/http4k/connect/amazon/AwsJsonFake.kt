package org.http4k.connect.amazon

import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.format.AutoMarshalling
import org.http4k.routing.bind
import org.http4k.routing.header
import se.ansman.kotshi.JsonSerializable

class AwsJsonFake(val autoMarshalling: AutoMarshalling, val awsService: AwsService) {
    inline fun <reified Req : Any> route(
        crossinline responseFn: (Any) -> Response = {
            Response(OK).body(autoMarshalling.asFormatString(it))
        },
        crossinline errorFn: (JsonError) -> Response = {
            Response(BAD_REQUEST).body(autoMarshalling.asFormatString(it))
        },
        crossinline fn: (Req) -> Any?
    ) =
        header("X-Amz-Target", "${awsService}.${Req::class.simpleName!!.calculateOperationName<Req>()}") bind {
            fn(autoMarshalling.asA(it.bodyString(), Req::class))
                ?.let {
                    when (it) {
                        is Unit -> Response(OK).body("{}")
                        is JsonError -> errorFn(it)
                        else -> responseFn(it)
                    }
                }
                ?: JsonError("ResourceNotFoundException", "$awsService can't find the specified item.").let(errorFn)
        }

    inline fun <reified Req : Any> String.calculateOperationName() = when {
        endsWith("Request") -> removeSuffix("Request")
        endsWith("Response") -> removeSuffix("Response")
        else -> Req::class.simpleName
    }
}

@JsonSerializable
data class JsonError(val __type: String, val Message: String)
