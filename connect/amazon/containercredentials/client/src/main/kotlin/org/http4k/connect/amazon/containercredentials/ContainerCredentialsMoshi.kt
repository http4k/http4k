package org.http4k.connect.amazon.containercredentials

import com.squareup.moshi.JsonAdapter
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import se.ansman.kotshi.KotshiJsonAdapterFactory

object ContainerCredentialsMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(ContainerCredentialsAdapterFactory)
        .done()
)


@KotshiJsonAdapterFactory
internal object ContainerCredentialsAdapterFactory : JsonAdapter.Factory by KotshiContainerCredentialsAdapterFactory
