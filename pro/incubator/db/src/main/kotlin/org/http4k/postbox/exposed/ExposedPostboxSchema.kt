package org.http4k.postbox.exposed

import org.http4k.db.ExposedTransactor
import org.http4k.db.Transactor
import org.jetbrains.exposed.sql.SchemaUtils
import javax.sql.DataSource

object ExposedPostboxSchema {

    fun create(dataSource: DataSource) {
        ExposedTransactor(dataSource) { }.perform {
            SchemaUtils.create(ExposedPostbox.Companion.PostboxTable)
        }
    }

    fun printCreateStatements(dataSource: DataSource) {
        ExposedTransactor(dataSource) { }.perform(Transactor.Mode.ReadOnly) {
            SchemaUtils.createStatements(ExposedPostbox.Companion.PostboxTable).joinToString("\n").also(::println)
        }
    }
}
