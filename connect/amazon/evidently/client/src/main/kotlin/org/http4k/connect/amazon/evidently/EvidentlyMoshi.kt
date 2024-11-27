package org.http4k.connect.amazon.evidently

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationContext
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object EvidentlyMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(EvidentlyJsonAdapterFactory)
        .value(ProjectName)
        .value(FeatureName)
        .value(EvaluationContext)
        .value(VariationName)
        .value(EntityId)
        .done()
)

@KotshiJsonAdapterFactory
object EvidentlyJsonAdapterFactory : JsonAdapter.Factory by KotshiEvidentlyJsonAdapterFactory
