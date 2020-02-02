package org.http4k.format

import kotlinx.serialization.json.JsonConfiguration.Companion.Stable
import kotlinx.serialization.modules.EmptyModule

/**
 * To implement custom JSON configuration, create your own object singleton extending
 * ConfigurableKotlinxSerialization.
 */
object KotlinxSerialization : ConfigurableKotlinxSerialization(Stable, EmptyModule)
