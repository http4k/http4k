package org.http4k.format

import org.apache.fory.ThreadSafeFory
import org.apache.fory.kotlin.ForyKotlin
import org.http4k.lens.BiDiMapping
import java.time.ZoneId

fun standardConfig(): AutoMappingConfiguration<ThreadSafeFory> = ForyKotlin.builder()
    .withXlang(false)
    .requireClassRegistration(true)
    .buildThreadSafeFory().asConfigurable()
    .withStandardMappings()
    .text(BiDiMapping(zoneRegion, ZoneId::of, ZoneId::toString))

@Suppress("UNCHECKED_CAST")
private val zoneRegion = ZoneId.of("Europe/London").javaClass

inline fun <reified T : Any> AutoMappingConfiguration<ThreadSafeFory>.register() = apply {
    done().register(T::class.java, T::class.java.name)
}

fun Fory(configureFn: AutoMappingConfiguration<ThreadSafeFory>.() -> AutoMappingConfiguration<ThreadSafeFory>) =
    ConfigurableFory(standardConfig().let(configureFn).done())
