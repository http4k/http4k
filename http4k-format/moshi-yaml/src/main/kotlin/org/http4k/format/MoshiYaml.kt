package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import java.lang.reflect.Type

private fun standardConfig() = Builder()
    .addLast(ListAdapter)
    .addLast(NullSafeMapAdapter)
    .asConfigurable()
    .withStandardMappings()

object MoshiYaml : ConfigurableMoshiYaml(standardConfig().done()) {
    fun update(
        configureFn: AutoMappingConfiguration<Builder>.() -> AutoMappingConfiguration<Builder>
    ) = ConfigurableMoshiYaml(standardConfig().let(configureFn).done())
}

/**
 * A special Adapter to serialise nulls
 */
object NullSafeMapAdapter : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi) =
        MapAdapter.create(type, annotations, moshi)?.serializeNulls()
}
