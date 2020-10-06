package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.http4k.core.MimeTypes
import org.junit.jupiter.api.Test

class LoadMetaResouceTest {

    @Test
    fun `can load file from meta-dir`() {
        assertThat(loadMetaResource<MimeTypes>("mime.types"), present())
    }

    @Test
    fun `throws on missing`() {
        assertThat(
            { loadMetaResource<MimeTypes>("myfile.txt") },
            throws(
                has(
                    java.lang.IllegalStateException::message,
                    equalTo(
                        "" +
                            "Could not find 'myfile.txt' inside META-INF. If using Shadow JAR, add mergeServiceFiles() to the configuration"
                    )
                )
            )
        )
    }
}
