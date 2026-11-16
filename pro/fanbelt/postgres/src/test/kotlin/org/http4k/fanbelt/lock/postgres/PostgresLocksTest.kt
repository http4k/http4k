/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock.postgres

import org.http4k.fanbelt.lock.LocksContract
import org.http4k.fanbelt.store.postgres.postgresUrl
import java.sql.DriverManager

class PostgresLocksTest : LocksContract() {
    override val locks = PostgresLocks { DriverManager.getConnection(postgresUrl) }
    override val elsewhere = PostgresLocks { DriverManager.getConnection(postgresUrl) }
}
