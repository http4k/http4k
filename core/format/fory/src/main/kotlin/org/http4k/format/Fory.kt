package org.http4k.format

import org.apache.fory.ThreadSafeFory
import org.apache.fory.kotlin.ForyKotlin

fun standardConfig(): AutoMappingConfiguration<ThreadSafeFory> = ForyKotlin.builder()
    .withXlang(false)
    .requireClassRegistration(false)
    .buildThreadSafeFory().asConfigurable()
    .withStandardMappings()

object Fory : ConfigurableFory(standardConfig().done()) {
    fun custom(configureFn: AutoMappingConfiguration<ThreadSafeFory>.() -> AutoMappingConfiguration<ThreadSafeFory>) =
        ConfigurableFory(standardConfig().let(configureFn).done())
}
