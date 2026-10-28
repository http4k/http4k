package org.http4k.webdriver.datastar

/**
 * A datastar attribute as written: data-[name]:[key]__[modifiers]="[expression]".
 */
data class DatastarAttribute(
    val qualifiedName: String,
    val name: String,
    val key: String?,
    val modifiers: Set<String>,
    val expression: String,
)
