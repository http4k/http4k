package org.http4k.serverless

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Status
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.serverless.BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS
import org.junit.jupiter.api.Test

class BootstrapAppLoaderTest {
    @Test
    fun `loads the expected app if it implements the AppLoader interface`() {
        val app = BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to TestApp::class.java.name), RequestContexts())
        assertThat(app(Request(Method.GET, "/")), hasStatus(Status.CREATED).and(hasHeader(HTTP4K_BOOTSTRAP_CLASS, TestApp::class.java.name)))
    }

    @Test
    fun `loads the expected app if it implements the AppLoaderWithContexts interface`() {
        val app = BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to TestAppWithContexts::class.java.name), RequestContexts())
        assertThat(app(Request(Method.GET, "/")), hasStatus(Status.CREATED).and(hasHeader(HTTP4K_BOOTSTRAP_CLASS, TestAppWithContexts::class.java.name)))
    }

    @Test
    fun `complains if the configured class is not found`() {
        assertThat({
            BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to "java.lang.NotAnApp"), RequestContexts())
        }, throws(isA<CouldNotFindAppLoaderException>()))
    }

    @Test
    fun `complains if the configured class is not an AppLoader`() {
        assertThat({
            BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to "java.lang.String"), RequestContexts())
        }, throws(isA<InvalidAppLoaderException>()))
    }
}
