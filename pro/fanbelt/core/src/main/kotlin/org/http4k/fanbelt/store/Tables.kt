/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import kotlin.reflect.KClass

interface Tables {
    operator fun <ID : Any, T : Identified<ID>> invoke(name: TableName, type: KClass<T>): Table<ID, T>
}

inline operator fun <ID : Any, reified T : Identified<ID>> Tables.invoke(name: TableName) = this(name, T::class)
