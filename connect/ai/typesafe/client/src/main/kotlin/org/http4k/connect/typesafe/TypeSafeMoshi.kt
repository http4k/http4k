package org.http4k.connect.typesafe

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.lang.reflect.Type

object TypeSafeMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(TypeSafeJsonAdapterFactory)
        .add(NullPreservingMoshiNodeAdapter)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(QuestionId)
        .value(Probability)
        .value(Confidence)
        .done()
)

object NullPreservingMoshiNodeAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi) =
        MoshiNodeAdapter.create(type, annotations, moshi)?.serializeNulls()
}

@KotshiJsonAdapterFactory
object TypeSafeJsonAdapterFactory : JsonAdapter.Factory by KotshiTypeSafeJsonAdapterFactory
