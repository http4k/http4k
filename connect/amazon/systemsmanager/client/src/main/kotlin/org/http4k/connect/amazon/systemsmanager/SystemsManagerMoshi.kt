package org.http4k.connect.amazon.systemsmanager

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.systemsmanager.model.SSMParameterName
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SystemsManagerMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(SystemsManagerJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .withSystemsManagerMappings()
        .done()
)

fun <T> AutoMappingConfiguration<T>.withSystemsManagerMappings() = apply {
    value(SSMParameterName)
}

@KotshiJsonAdapterFactory
object SystemsManagerJsonAdapterFactory : JsonAdapter.Factory by KotshiSystemsManagerJsonAdapterFactory
