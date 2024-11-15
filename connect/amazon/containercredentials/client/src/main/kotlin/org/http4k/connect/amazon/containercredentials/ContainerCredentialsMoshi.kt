package org.http4k.connect.amazon.containercredentials

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.asConfigurable
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object ContainerCredentialsMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AwsCoreJsonAdapterFactory())
        .add(ContainerCredentialsAdapterFactory)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .done()
)


@KotshiJsonAdapterFactory
internal object ContainerCredentialsAdapterFactory : JsonAdapter.Factory by KotshiContainerCredentialsAdapterFactory
