package org.http4k.serverless

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.serverless.BootstrapAppLoader.HTTP4K_BOOTSTRAP_CLASS
import org.junit.Test

class BootstrapAppLoaderTest {

    @Test
    fun `loads the expected app`() {
        val app = BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to TestApp::class.java.name))
        app(Request(Method.GET, "/")) shouldMatch hasStatus(Status.CREATED).and(hasHeader(HTTP4K_BOOTSTRAP_CLASS, TestApp::class.java.name))
    }

    @Test
    fun `complains if the configured class is not found`() {
        assertThat({
            BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to "java.lang.NotAnApp"))
        }, throws(equalTo(BootstrapException("Could not find AppLoader class: java.lang.NotAnApp"))))
    }

    @Test
    fun `complains if the configures class is not an AppLoader`() {
        assertThat({
            BootstrapAppLoader(mapOf(HTTP4K_BOOTSTRAP_CLASS to "java.lang.String"))
        }, throws(equalTo(BootstrapException("AppLoader class should be an object singleton that implements org.http4k.serverless.AppLoader"))))
    }
}