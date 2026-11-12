/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.exposed

import org.http4k.fanbelt.store.sql
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.format.Moshi
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class RowLevelSecurityTest {
    private val mine = TenantId.of("rls-mine-${System.nanoTime()}")
    private val theirs = TenantId.of("rls-theirs-${System.nanoTime()}")

    @Test
    fun `a raw query that forgets the tenant still sees only the tenant's rows`() {
        provision()
        val storage = ExposedDocumentStorage(Database.connect(postgresUrl.substringBefore('?'), user = APP_USER), Moshi, PostgresDocuments)

        val seen = storage.write { tenants -> tenants.tables(mine).sql { ids(it.connection) } }

        assertEquals(setOf("$mine-1"), seen)
    }

    @Test
    fun `a connection that never set the tenant sees nothing`() {
        provision()

        val seen = DriverManager.getConnection(postgresUrl.substringBefore('?'), APP_USER, "").use(::ids)

        assertEquals(emptySet<String>(), seen)
    }

    private fun provision() = DriverManager.getConnection(postgresUrl).use { admin ->
        admin.createStatement().use {
            it.execute("create table if not exists rls_notes (tenant varchar(100) not null, id varchar(100) not null, primary key (tenant, id))")
            it.execute("alter table rls_notes enable row level security")
            it.execute("drop policy if exists tenant_rows on rls_notes")
            it.execute("create policy tenant_rows on rls_notes using (tenant = current_setting('fanbelt.tenant', true))")
            it.execute("do $$ begin if not exists (select from pg_roles where rolname = '$APP_USER') then create role $APP_USER login; end if; end $$")
            it.execute("grant select on rls_notes to $APP_USER")
            it.execute("insert into rls_notes values ('$mine', '$mine-1'), ('$theirs', '$theirs-1')")
        }
    }

    private fun ids(connection: Connection) = connection.createStatement().use { statement ->
        statement.executeQuery("select id from rls_notes").use { rows -> generateSequence { if (rows.next()) rows.getString(1) else null }.toSet() }
    }
}

private const val APP_USER = "fanbelt_rls_app"
