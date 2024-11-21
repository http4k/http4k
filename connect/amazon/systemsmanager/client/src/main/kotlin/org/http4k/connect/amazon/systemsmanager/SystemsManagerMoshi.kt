package org.http4k.connect.amazon.systemsmanager

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.systemsmanager.model.SSMParameterName
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SystemsManagerMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(SystemsManagerJsonAdapterFactory)
        .withSystemsManagerMappings()
        .done()
)

fun <T> AutoMappingConfiguration<T>.withSystemsManagerMappings() = apply {
    value(SSMParameterName)
}

@KotshiJsonAdapterFactory
object SystemsManagerJsonAdapterFactory : JsonAdapter.Factory by KotshiSystemsManagerJsonAdapterFactory
