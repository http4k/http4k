package org.http4k.postbox.storage.exposed

import org.http4k.postbox.storage.PostboxContract
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.junit.jupiter.api.BeforeEach

class ExposedPostboxTest : PostboxContract() {

    @BeforeEach
    fun before() {
        postbox.perform { PostboxTable("test").deleteAll() }
    }

    override val postbox = PostboxTransactor(dataSource, timeSource, "test")

    companion object {
        val dataSource by lazy { postgresDataSource("test") }
    }
}
