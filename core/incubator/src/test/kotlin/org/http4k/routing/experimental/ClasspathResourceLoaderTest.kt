package org.http4k.routing.experimental

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri.Companion.of
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ClasspathResourceLoaderTest : ResourceLoaderContract(ResourceLoaders.Classpath("/")) {

    @Disabled
    override fun `loads embedded index file`() {
        super.`loads embedded index file`()
    }

    @Test
    fun `cannot escape base package`() {
        val loader = ResourceLoaders.Classpath("/org")
        val request = Request(GET, of("/../mybob.xml"))
        assertThat(loader.match(request)(request), hasStatus(NOT_FOUND))
    }
}
