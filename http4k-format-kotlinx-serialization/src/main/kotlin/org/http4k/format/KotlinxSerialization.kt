package org.http4k.format

/**
 * To implement custom JSON configuration, create your own object singleton extending
 * ConfigurableKotlinxSerialization, passing in the JSON configuration block
 */
object KotlinxSerialization : ConfigurableKotlinxSerialization({})
