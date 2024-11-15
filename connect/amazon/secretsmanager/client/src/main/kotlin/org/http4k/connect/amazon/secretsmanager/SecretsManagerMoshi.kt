package org.http4k.connect.amazon.secretsmanager

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.amazon.secretsmanager.model.VersionStage
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

object SecretsManagerMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(SecretsManagerJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
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

