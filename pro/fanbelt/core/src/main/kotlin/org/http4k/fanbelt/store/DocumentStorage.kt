/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

interface DocumentStorage {
    fun <R> write(work: (Tenants) -> R): R

    fun <R> read(work: (Tenants) -> R): R = write(work)
}
