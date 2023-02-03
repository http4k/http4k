package org.http4k.format

import org.http4k.format.KotlinxSerialization.asJsonObject

/**
 * To implement custom JSON configuration, create your own object singleton extending
 * ConfigurableKotlinxSerialization, passing in the JSON configuration block
 */
object KotlinxSerialization : ConfigurableKotlinxSerialization({
    ignoreUnknownKeys = true
    asConfigurable().withStandardMappings()
        .done()
})
