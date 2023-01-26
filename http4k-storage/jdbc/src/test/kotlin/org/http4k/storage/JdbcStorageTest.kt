package org.http4k.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.format.Moshi
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class JdbcStorageTest : StorageContract() {
    private val name = UUID.randomUUID().toString()

    override val storage: Storage<AnEntity> by lazy {
        val ds = HikariDataSource(
            HikariConfig().apply {
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1"
            })

        transaction(Database.connect(ds)) {
            SchemaUtils.create(StorageTable("AnEntity"))
        }

        Storage.Jdbc(ds, Moshi)
    }
}
