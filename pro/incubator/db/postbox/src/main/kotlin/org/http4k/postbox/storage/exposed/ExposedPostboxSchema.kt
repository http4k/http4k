package org.http4k.postbox.storage.exposed

import org.http4k.db.Transactor
import org.http4k.db.exposed.ExposedTransactor
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import javax.sql.DataSource

object ExposedPostboxSchema {

    fun create(dataSource: DataSource, prefix: String = "http4k") {
        ExposedTransactor(dataSource) { }.perform {
            SchemaUtils.create(PostboxTable(prefix))
        }
    }

    fun printCreateStatements(dataSource: DataSource, prefix: String = "http4k") {
        ExposedTransactor(dataSource) { }.perform(Transactor.Mode.ReadOnly) {
            SchemaUtils.createStatements(PostboxTable(prefix)).joinToString("\n").also(::println)
        }
    }
}
