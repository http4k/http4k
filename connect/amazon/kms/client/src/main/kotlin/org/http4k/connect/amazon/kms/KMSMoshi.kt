package org.http4k.connect.amazon.kms

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object KMSMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(KMSJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .done()
)

@KotshiJsonAdapterFactory
object KMSJsonAdapterFactory : JsonAdapter.Factory by KotshiKMSJsonAdapterFactory
