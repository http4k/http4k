package org.http4k.webdriver.datastar

/**
 * A piece of datastar the DatastarWebDriver models, after datastar's own plugin kinds. The
 * built-ins are always there; an app's plugins add to them, or replace the built-in of the same name.
 */
sealed interface DatastarPlugin

internal object DatastarPlugins {
    val standard: List<DatastarPlugin> = listOf(Signals, Indicator, Computed, Text, Show, Classes, Attrs, Bind, On, Init, IgnoreMorph)
}
