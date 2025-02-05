package org.http4k.postbox.exposed

import org.http4k.postbox.PostboxContract
import org.jetbrains.exposed.sql.deleteAll
import org.junit.jupiter.api.BeforeEach

class ExposedPostboxTest : PostboxContract() {
    @BeforeEach
    fun before() {
        postbox.perform { PostboxTable("test").deleteAll() }
    }

    override val postbox = PostboxTransactor(postgresDataSource("test"), "test")
}
