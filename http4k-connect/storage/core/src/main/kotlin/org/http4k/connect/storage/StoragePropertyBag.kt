package org.http4k.connect.storage

import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.jvmErasure

/**
 * Provide a dynamic-static linkage into storage
 */
open class StoragePropertyBag(
    private val storage: Storage<String>,
    private val autoMarshalling: AutoMarshalling = Moshi
) {
    protected fun <OUT : Any?> item(default: OUT? = null) = object : ReadWriteProperty<StoragePropertyBag, OUT> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: StoragePropertyBag, property: KProperty<*>): OUT {
            val stored = storage[property.name]

            return when {
                stored == null -> when {
                    property.returnType.isMarkedNullable -> default
                    default != null -> default
                    else -> throw NoSuchElementException("Field <${property.name}> is missing")
                }

                else -> autoMarshalling.asA(stored, property.returnType.jvmErasure)
            } as OUT
        }

        override fun setValue(thisRef: StoragePropertyBag, property: KProperty<*>, value: OUT) {
            when (value) {
                null -> storage.remove(property.name)
                else -> storage[property.name] = autoMarshalling.asFormatString(value)
            }
        }
    }
}

