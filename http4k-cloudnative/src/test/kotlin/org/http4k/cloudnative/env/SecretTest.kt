package org.http4k.cloudnative.env

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class SecretTest {

    private val aSecret = Secret("mySecret")

    @Test
    fun equality() {
        assertThat(Secret("mySecret".toByteArray()), equalTo(aSecret))
        assertThat(aSecret.hashCode(), equalTo(aSecret.hashCode()))
    }

    @Test
    fun `can use the value, after which it is cleared`() {
        val inputBytes = "mySecret".toByteArray()
        val secretWithBytes = Secret(inputBytes)

        assertEqualTo(secretWithBytes, "mySecret".toByteArray())
        assertThat(secretWithBytes.use { it }, equalTo("mySecret"))
        assertEqualTo(secretWithBytes, ByteArray(0))

        assertThat(inputBytes.size, equalTo("mySecret".length))
        assertThat(inputBytes.toList(), allElements(equalTo(0.toByte())))
    }

    @Test
    fun `using the value twice throws up`() {
        aSecret.use { it }
        assertThat({ aSecret.use { it.also {} } }, throws<IllegalStateException>())
    }

    @Test
    fun `toString value doesn't reveal value`() {
        assertThat(aSecret.toString(), equalTo("Secret(hashcode = 1666631293)"))
    }

    private fun assertEqualTo(secret: Secret, bytes: ByteArray) {
        val field = secret::class.java.declaredFields[0]
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        assertTrue((field.get(secret) as AtomicReference<ByteArray>).get()!!.contentEquals(bytes))
    }
}
