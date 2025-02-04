package org.http4k.postbox.exposed

import org.http4k.db.ExposedTransactor
import org.http4k.postbox.PostboxTransactor
import javax.sql.DataSource

fun PostboxTransactor(dataSource: DataSource, tablePrefix: String = "http4k"): PostboxTransactor =
    ExposedTransactor(dataSource, { ExposedPostbox(tablePrefix) })
