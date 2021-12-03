package org.http4k.routing.experimental

import org.junit.jupiter.api.Disabled

class ClasspathResourceLoaderTest : ResourceLoaderContract(ResourceLoaders.Classpath("/")) {

    @Disabled
    override fun `loads embedded index file`() {
        super.`loads embedded index file`()
    }
}
