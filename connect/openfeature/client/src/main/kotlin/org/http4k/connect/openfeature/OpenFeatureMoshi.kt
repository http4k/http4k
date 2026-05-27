package org.http4k.connect.openfeature

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.TargetingKey
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object OpenFeatureMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(OpenFeatureJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withOpenFeatureMappings()
        .done()
)

@KotshiJsonAdapterFactory
object OpenFeatureJsonAdapterFactory : JsonAdapter.Factory by KotshiOpenFeatureJsonAdapterFactory

fun <T> AutoMappingConfiguration<T>.withOpenFeatureMappings() = apply {
    value(FlagKey)
    value(TargetingKey)
}
