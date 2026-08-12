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

/**
 * Fory looks up serializers by exact runtime class, so mapping the abstract [ZoneId] is not enough -
 * every non-offset zone is really a package-private java.time.ZoneRegion.
 */
@Suppress("UNCHECKED_CAST")
private val zoneRegion = ZoneId.of("Europe/London").javaClass

/**
 * Register a type which Fory should marshal reflectively. Registering by qualified name rather than
 * auto-assigned id keeps the wire format independent of registration order across peers.
 */
inline fun <reified T : Any> AutoMappingConfiguration<ThreadSafeFory>.register() = apply {
    done().register(T::class.java, T::class.java.name)
}

/**
 * All marshalled types must be declared up-front, either with [register] or - for tiny types and
 * other custom mappings - with the standard [AutoMappingConfiguration] methods.
 */
fun Fory(configureFn: AutoMappingConfiguration<ThreadSafeFory>.() -> AutoMappingConfiguration<ThreadSafeFory>) =
    ConfigurableFory(standardConfig().let(configureFn).done())
