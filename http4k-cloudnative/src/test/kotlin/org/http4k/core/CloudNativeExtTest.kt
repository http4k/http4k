package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.cloudnative.env.Authority
import org.http4k.cloudnative.env.Host
import org.http4k.cloudnative.env.Port
import org.junit.jupiter.api.Test

class CloudNativeExtTest {

    @Test
    fun `uri extensions`() {
        val originalUri = Uri.of("http://bob:80")

        val newHost = Host("hello")
        with(originalUri.host(newHost)) {
            assertThat(this, equalTo(Uri.of("http://hello:80")))
            assertThat(host(), equalTo(newHost))
            assertThat(host().asAuthority(), equalTo(Authority(host())))
        }

        val newPort = Port(81)
        with(originalUri.port(newPort)) {
            assertThat(this, equalTo(Uri.of("http://bob:81")))
            assertThat(port(), equalTo(newPort))
        }

        val newAuthority = Authority(Host.localhost, Port(82))
        with(originalUri.authority(newAuthority)) {
            assertThat(this, equalTo(Uri.of("http://localhost:82")))
            assertThat(authority(), equalTo(newAuthority))
        }
    }
}
