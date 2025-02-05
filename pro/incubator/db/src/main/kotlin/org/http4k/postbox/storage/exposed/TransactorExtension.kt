package org.http4k.postbox.storage.exposed

import dev.forkhandles.time.TimeSource
import dev.forkhandles.time.systemTime
import org.http4k.db.ExposedTransactor
import org.http4k.postbox.PostboxTransactor
import javax.sql.DataSource

fun PostboxTransactor(
    dataSource: DataSource,
    timeSource: TimeSource = systemTime,
    tablePrefix: String = "http4k"
): PostboxTransactor =
    ExposedTransactor(dataSource, { ExposedPostbox(tablePrefix, timeSource) })
