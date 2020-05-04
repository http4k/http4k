package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestResources::class)
class TestResourcesTest {
    @Test
    fun `can load resource that exists`(resourceLoader: ResourceLoader) {
        assertThat(resourceLoader.text("file.txt").trim(), equalTo("content"))
        assertThat(resourceLoader.stream("file.txt").reader().readText().trim(), equalTo("content"))
        assertThat(String(resourceLoader.bytes("file.txt")).trim(), equalTo("content"))
    }

    @Test
    fun `cannot load resource that does not exist`(resourceLoader: ResourceLoader) {
        assertThat({resourceLoader.text("file.txt") }, throws<IllegalStateException>())
        assertThat({resourceLoader.stream("file.txt") }, throws<IllegalStateException>())
        assertThat({resourceLoader.bytes("file.txt") }, throws<IllegalStateException>())
    }
}
