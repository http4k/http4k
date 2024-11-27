package org.http4k.connect.amazon.secretsmanager

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.amazon.secretsmanager.model.VersionStage
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SecretsManagerMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(SecretsManagerJsonAdapterFactory)
        .withSecretsManagerMappings()
        .done()
)

fun <T> AutoMappingConfiguration<T>.withSecretsManagerMappings() = apply {
    value(SecretId)
    value(VersionId)
    value(VersionStage)
}

@KotshiJsonAdapterFactory
object SecretsManagerJsonAdapterFactory : JsonAdapter.Factory by KotshiSecretsManagerJsonAdapterFactory

