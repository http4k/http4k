package org.http4k.connect.amazon.apigateway

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.core.Response
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import kotlin.reflect.KClass

abstract class AwsApiGatewayAction<R : Any>(private val clazz: KClass<R>) : Action<Result<R, RemoteFailure>> {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(ApiGatewayJackson.asA(bodyString().let { if (it.isEmpty()) "{}" else it }, clazz))
            else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString()))
        }
    }
}

object ApiGatewayJackson : ConfigurableJackson(KotlinModule.Builder().build()
    .asConfigurable()
    .withStandardMappings()
    .done()
    .deactivateDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .apply {
        configOverride(List::class.java).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
    }
)
