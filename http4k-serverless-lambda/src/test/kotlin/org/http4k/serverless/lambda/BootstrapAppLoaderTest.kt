package org.http4k.serverless.lambda

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

class BootstrapAppLoaderTest {
    @Test
    fun `complains if the configured class is not found`() {
        assertThat({
            BootstrapAppLoader(mapOf(BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS to "java.lang.NotAnApp"))
        }, throws(equalTo(BootstrapException("Could not find AppLoader class: java.lang.NotAnApp"))))
    }

    @Test
    fun `complains if the configures class is not an AppLoader`() {
        assertThat({
            BootstrapAppLoader(mapOf(BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS to "java.lang.String"))
        }, throws(equalTo(BootstrapException("AppLoader class should be an object singleton that implements org.http4k.serverless.lambda.AppLoader"))))
    }
}