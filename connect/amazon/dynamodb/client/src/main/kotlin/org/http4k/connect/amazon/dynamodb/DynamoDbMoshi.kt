package org.http4k.connect.amazon.dynamodb

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.ClientToken
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.NextToken
import org.http4k.connect.amazon.dynamodb.model.TableName
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

private fun standardConfig() = Moshi.Builder()
    .add(DynamoDbJsonAdapterFactory)
    .add(AwsCoreJsonAdapterFactory())
    .add(MapAdapter)
    .add(ListAdapter)
    .asConfigurable()
    .withStandardMappings()
    .withAwsCoreMappings()
    .value(AttributeName)
    .value(IndexName)
    .value(TableName)
    .value(ClientToken)
    .value(NextToken)

object DynamoDbMoshi : ConfigurableMoshi(
    standardConfig().done()
) {
    fun update(
        configureFn: AutoMappingConfiguration<Moshi.Builder>.() -> AutoMappingConfiguration<Moshi.Builder>
    ) = ConfigurableMoshi(standardConfig().let(configureFn).done())
}

@KotshiJsonAdapterFactory
object DynamoDbJsonAdapterFactory : JsonAdapter.Factory by KotshiDynamoDbJsonAdapterFactory
