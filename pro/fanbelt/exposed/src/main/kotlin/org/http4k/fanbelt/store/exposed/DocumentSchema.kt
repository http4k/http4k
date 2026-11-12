/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.TableName
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

class DocumentSchema(name: TableName, seqColumn: Table.(String) -> Column<Long>, docColumn: Table.(String) -> Column<String>) : Table(name.value) {
    val tenant = varchar("tenant", 100)
    val id = varchar("id", 255)
    val seq = seqColumn("seq")
    val doc = docColumn("doc")

    override val primaryKey = PrimaryKey(tenant, id)

    init {
        index(false, tenant, seq)
    }
}
