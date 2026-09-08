/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.storage.jdbc

import dev.forkhandles.time.TimeSource
import dev.forkhandles.time.systemTime
import dev.forkhandles.tx.jdbc.JdbcTransactor
import org.http4k.postbox.TransactionalPostbox
import javax.sql.DataSource

fun PostboxTransactor(
    dataSource: DataSource,
    timeSource: TimeSource = systemTime,
    tablePrefix: String = "http4k"
): TransactionalPostbox =
    JdbcTransactor({ dataSource.connection }, { JdbcPostbox(dataSource, tablePrefix, timeSource) })
