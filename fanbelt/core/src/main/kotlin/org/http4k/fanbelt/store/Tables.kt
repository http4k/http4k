package org.http4k.fanbelt.store

import dev.forkhandles.values.Value
import kotlin.reflect.KClass

/**
 * One tenant's tables within a transaction, reached through Tenants.tables.
 */
interface Tables {
    operator fun <ID : Value<*>, T : Identified<ID>> invoke(name: TableName, type: KClass<T>): Table<ID, T>
}

inline operator fun <ID : Value<*>, reified T : Identified<ID>> Tables.invoke(name: TableName) = this(name, T::class)
