/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

/**
 * One database of tenant documents, reached through transactions: write for operations, which may touch only one
 * tenant, and read for everything a state reads. Work that fails on a conflict is retried, so it may run more than
 * once.
 */
interface DocumentStorage {
    fun <R> write(work: (Tenants) -> R): R

    fun <R> read(work: (Tenants) -> R): R = write(work)
}
