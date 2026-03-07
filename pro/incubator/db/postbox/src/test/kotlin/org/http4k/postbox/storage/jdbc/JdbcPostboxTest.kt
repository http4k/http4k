package org.http4k.postbox.storage.jdbc

import org.http4k.postbox.storage.PostboxContract
import org.junit.jupiter.api.BeforeEach

class JdbcPostboxTest : PostboxContract() {

    @BeforeEach
    fun before() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { it.execute("DELETE FROM test_postbox") }
        }
    }

    override val postbox = PostboxTransactor(dataSource, timeSource, "test")

    companion object {
        val dataSource by lazy { postgresDataSource("test") }
    }
}

