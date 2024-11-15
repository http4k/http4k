package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CredentialsChainTest {

    private val credentials123 = AwsCredentials("key123", "secret123")
    private val credentials456 = AwsCredentials("key456", "secret456")

    @Test
    fun `second element has credentials`() {
        val chain = CredentialsChain { null } orElse CredentialsChain { credentials456 }

        assertThat(
            chain.invoke(),
            equalTo(credentials456)
        )
    }

    @Test
    fun `first element has credentials`() {
        val chain = CredentialsChain { credentials123 } orElse CredentialsChain { credentials456 }

        assertThat(
            chain.invoke(),
            equalTo(credentials123)
        )
    }

    @Test
    fun `no element has credentials`() {
        val chain = CredentialsChain { null } orElse CredentialsChain { null }

        assertThat(
            chain.invoke(),
            absent()
        )
    }

    @Test
    fun `as provider - with credentials`() {
        val provider = CredentialsChain { credentials123 }.provider()

        assertThat(
            provider.invoke(),
            equalTo(credentials123)
        )
    }

    @Test
    fun `as provider no credentials`() {
        val provider = CredentialsChain { null }.provider()

        assertThrows<java.lang.IllegalArgumentException> {
            provider.invoke()
        }
    }
}
