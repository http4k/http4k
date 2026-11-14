package org.http4k.connect.openfga

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.ContinuationToken
import org.http4k.connect.openfga.model.CorrelationId
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.StoreName
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object OpenFgaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(OpenFgaJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withOpenFgaMappings()
        .done()
)

@KotshiJsonAdapterFactory
object OpenFgaJsonAdapterFactory : JsonAdapter.Factory by KotshiOpenFgaJsonAdapterFactory

fun <T> AutoMappingConfiguration<T>.withOpenFgaMappings() = apply {
    value(StoreId)
    value(StoreName)
    value(AuthorizationModelId)
    value(ContinuationToken)
    value(CorrelationId)
    value(TypeName)
    value(Relation)
    value(User)
    value(FgaObject)
}
