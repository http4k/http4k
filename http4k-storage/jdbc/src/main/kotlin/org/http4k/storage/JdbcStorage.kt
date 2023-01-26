package org.http4k.storage

import org.http4k.format.AutoMarshalling
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import javax.sql.DataSource

/**
 * Database-backed storage implementation. Automatically marshals objects to
 * and from string-value format.
 */
inline fun <reified T : Any> Storage.Companion.Jdbc(
    dataSource: DataSource,
    autoMarshalling: AutoMarshalling,
    tableName: String = T::class.java.simpleName
): Storage<T> = object : Storage<T> {

    private val db = Database.connect(dataSource)

    private val table = StorageTable(tableName)

    override fun get(key: String) = tx {
        table.select { table.id eq key }.firstOrNull()?.let { autoMarshalling.asA<T>(it[table.contents]) }
    }

    override fun set(key: String, data: T) {
        tx {
            when (table.select { table.id eq key }.count()) {
                0L -> table.insert {
                    it[table.id] = key
                    it[contents] = autoMarshalling.asFormatString(data)
                }

                else -> table.update({ table.id eq key }) {
                    it[contents] = autoMarshalling.asFormatString(data)
                } > 0
            }
        }
    }

    override fun remove(key: String) = tx {
        table.deleteWhere { table.id eq key } > 0
    }

    override fun keySet(keyPrefix: String) = tx {
        when {
            keyPrefix.isBlank() -> table.selectAll()
            else -> table.select { table.id like "$keyPrefix%" }
        }.map { it[table.id] }.toSet()
    }

    override fun removeAll(keyPrefix: String) = tx {
        when {
            keyPrefix.isBlank() -> table.deleteAll().run { true }
            else -> table.deleteWhere { table.id like "$keyPrefix%" } > 0
        }
    }

    private fun <T> tx(statement: Transaction.() -> T): T = transaction(db) {
        statement()
    }
}

open class StorageTable(name: String = "") : Table(name) {
    val id = varchar("key", 500)
    val contents: Column<String> = text("contents")
    override val primaryKey = PrimaryKey(id, name = name + "_pk")
}
