package org.http4k.connect.amazon.evidently

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationContext
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object EvidentlyMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(EvidentlyJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .value(ProjectName)
        .value(FeatureName)
        .value(EvaluationContext)
        .value(VariationName)
        .value(EntityId)
        .done()
)

@KotshiJsonAdapterFactory
object EvidentlyJsonAdapterFactory : JsonAdapter.Factory by KotshiEvidentlyJsonAdapterFactory
