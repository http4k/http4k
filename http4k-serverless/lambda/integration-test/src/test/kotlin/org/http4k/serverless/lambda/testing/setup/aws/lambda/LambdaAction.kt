package org.http4k.serverless.lambda.testing.setup.aws.lambda

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.serverless.lambda.testing.setup.aws.Action
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure
import kotlin.reflect.KClass

abstract class LambdaAction<R : Any>(private val clazz: KClass<R>) : Action<Result<R, RemoteFailure>> {
    override fun toResult(response: Response) =
        response.toActionResult(toRequest())
            .map { LambdaJackson.asA(response.bodyString().let { if (it.isEmpty()) "{}" else it }, clazz) }
}

fun Response.toActionResult(originalRequest: Request) = with(this) {
    when {
        status.successful -> Success(this)
        else -> Failure(RemoteFailure(originalRequest.method, originalRequest.uri, status, bodyString()))
    }
}

object LambdaJackson : ConfigurableJackson(KotlinModule()
    .asConfigurable()
    .withStandardMappings()
    .done()
    .deactivateDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
)
