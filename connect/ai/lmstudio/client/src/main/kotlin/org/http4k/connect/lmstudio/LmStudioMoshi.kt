package org.http4k.connect.lmstudio

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Timestamp
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object LmStudioMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(LmStudioJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .value(Base64Blob)
        .value(CompletionId)
        .value(ModelName)
        .value(Org)
        .value(ObjectType)
        .value(ObjectId)
        .value(Role)
        .value(Timestamp)
        .value(TokenId)
        .value(User)
        .value(ResponseFormatType)
        .done()
)

@KotshiJsonAdapterFactory
object LmStudioJsonAdapterFactory : JsonAdapter.Factory by KotshiLmStudioJsonAdapterFactory
