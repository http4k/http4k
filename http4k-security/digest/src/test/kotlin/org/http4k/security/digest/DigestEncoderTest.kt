package org.http4k.security.digest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.security.Nonce
import org.http4k.security.digest.Qop.Auth
import org.http4k.util.Hex.hex
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class DigestEncoderTest {

    private val encoder = DigestEncoder(MessageDigest.getInstance("MD5"), Charsets.ISO_8859_1)

    @Test
    fun `calculate Auth response 1`() {
        val response = encoder(
            realm = "super-secure IOT device",
            qop = Auth,
            nonce = Nonce("13379001"),
            digestUri = "/cgi-bin/magicBox.cgi?action=getSystemInfo",
            cnonce = Nonce("abcdef0123456789ab"),
            nonceCount = 1,
            method = POST,
            username = "root",
            password = "letmein"
        )

        assertThat(hex(response), equalTo("25eafbb371d56822a8aeb6b5107f38a9"))
    }

    @Test
    fun `calculate Auth response 2`() {
        val response = encoder(
            realm = "a random server",
            qop = Auth,
            nonce = Nonce("fedcba9999"),
            digestUri = "/cgi-bin/storage.cgi?action=getCaps",
            cnonce = Nonce("abcabcabcabc"),
            nonceCount = 1,
            method = GET,
            username = "root",
            password = "letmein"
        )

        assertThat(hex(response), equalTo("efe15f00a6b0ea7f552279c1409ed8d4"))
    }

    @Test
    fun `calculate no-qop response`() {
        val response = encoder(
            realm = "insecure-server",
            qop = null,
            nonce = Nonce("123"),
            digestUri = "/users/credentials",
            cnonce = null,
            nonceCount = null,
            method = GET,
            username = "admin",
            password = ""
        )

        assertThat(hex(response), equalTo("0d77aa99f5de4156f0b73cfea0d84169"))
    }
}
