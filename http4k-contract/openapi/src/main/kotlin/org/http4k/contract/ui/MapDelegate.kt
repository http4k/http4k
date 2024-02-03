package org.http4k.contract.ui

import kotlin.reflect.KProperty

internal class MapDelegate<T>(
    private val properties: MutableMap<String, UiProperty<out Any>>,
    private val name: String,
    private val format: (T) -> String? = { it?.toString() },
    default: T? = null
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = properties[name]?.value as T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (value == null) {
            properties.remove(name)
        } else {
            properties[name] = UiProperty(value, format(value))
        }
    }

    init {
        if (default != null) properties[name] = UiProperty(default, format(default))
    }
}

internal data class UiProperty<T>(val value: T, val formatted: String?)
