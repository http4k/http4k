package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class SecureEqualsTest {

    @Test
    fun `equal strings match`() {
        assertThat(secureEquals("abcdefgh", "abcdefgh"), equalTo(true))
    }

    @Test
    fun `same length differing strings do not match`() {
        assertThat(secureEquals("abcdefgh", "abcdefgX"), equalTo(false))
    }

    @Test
    fun `differing length strings do not match`() {
        assertThat(secureEquals("abc", "abcdef"), equalTo(false))
    }

    @Test
    fun `empty strings match`() {
        assertThat(secureEquals("", ""), equalTo(true))
    }

    @Test
    fun `nulls never match`() {
        assertThat(secureEquals(null, null), equalTo(false))
        assertThat(secureEquals(null, "abc"), equalTo(false))
        assertThat(secureEquals("abc", null), equalTo(false))
    }
}
