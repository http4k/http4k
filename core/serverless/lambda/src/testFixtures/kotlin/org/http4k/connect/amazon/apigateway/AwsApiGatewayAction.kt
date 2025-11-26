package org.http4k.connect.amazon.apigateway

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.core.Response
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import tools.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

abstract class AwsApiGatewayAction<R : Any>(private val clazz: KClass<R>) : Action<Result<R, RemoteFailure>> {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(ApiGatewayJackson.asA(bodyString().let { it.ifEmpty { "{}" } }, clazz))
            else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString()))
        }
    }
}

object ApiGatewayJackson : ConfigurableJackson(KotlinModule.Builder().build()
    .asConfigurable()
    .withStandardMappings()
    .done()
    .rebuild()
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
    .configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .build()
    // TODO fix this mapping!
//    .apply {
//        val a: JsonMapper? = this
//        configOverride(List::class.java).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
//    }
)
