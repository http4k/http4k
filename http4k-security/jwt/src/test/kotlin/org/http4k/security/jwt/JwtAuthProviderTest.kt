package org.http4k.security.jwt

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test

class JwtAuthProviderTest {

    private val hs = HsHelper("testServer", setOf("testApp"))
    private val rsa = RsaHelper("testServer", setOf("testApp"))

    @Test
    fun `process invalid jwt`() {
        assertThat(hs.verify("lolcats"), absent())
    }

    @Test
    fun `process local hs jwt`() {
        val token = hs.generate("sub1")
        val claims = hs.verify(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process local rsa jwt`() {
        val token = rsa.generate("sub1")
        val claims = rsa.verify(token)
        assertThat(claims, present())
        assertThat(claims!!.subject, equalTo("sub1"))
    }

    @Test
    fun `process remote jwt`() {
//        val token = rsa.generate("sub1")
//        val claims = rsa.verify(token)
//        assertThat(claims, present())
//        assertThat(claims!!.subject, equalTo("sub1"))

////        val pemObject = PemObject("RSA PRIVATE KEY", privateKey.encoded)
//
//        Files.write(Path("src/test/resources/keys/rsa_priv.key"), jwsKeyPair.private.encoded)
//        Files.write(Path("src/test/resources/keys/rsa_pub.key"), jwsKeyPair.public.encoded)
    }
}
