package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class SignalStoreTest {

    private val store = SignalStore()

    @Test
    fun `set and get nested paths`() {
        store["count"] = 1.0
        store["user.name"] = "bob"

        assertThat(store["count"], equalTo<Any?>(1.0))
        assertThat(store["user.name"], equalTo<Any?>("bob"))
        assertThat(store["user"], equalTo<Any?>(mapOf("name" to "bob")))
        assertThat(store["missing"], absent())
        assertThat(store["user.missing"], absent())
    }

    @Test
    fun `contains distinguishes missing from null`() {
        store["present"] = null
        assertThat(store.contains("present"), equalTo(true))
        assertThat(store.contains("missing"), equalTo(false))
        store["a.b"] = 1.0
        assertThat(store.contains("a.b"), equalTo(true))
        assertThat(store.contains("a.c"), equalTo(false))
    }

    @Test
    fun `patch deep merges`() {
        store["user.name"] = "bob"
        store.patch(mapOf("user" to mapOf("age" to 42.0), "count" to 1.0))

        assertThat(store["user.name"], equalTo<Any?>("bob"))
        assertThat(store["user.age"], equalTo<Any?>(42.0))
        assertThat(store["count"], equalTo<Any?>(1.0))
    }

    @Test
    fun `patch with null deletes the signal`() {
        store["count"] = 1.0
        store.patch(mapOf("count" to null))
        assertThat(store.contains("count"), equalTo(false))
    }

    @Test
    fun `patch onlyIfMissing does not overwrite or delete`() {
        store["count"] = 1.0
        store.patch(mapOf("count" to 99.0, "fresh" to true, "gone" to null), onlyIfMissing = true)

        assertThat(store["count"], equalTo<Any?>(1.0))
        assertThat(store["fresh"], equalTo<Any?>(true))
        assertThat(store.contains("gone"), equalTo(false))
    }

    @Test
    fun `renders json with integral numbers unadorned`() {
        store["count"] = 2.0
        store["ratio"] = 2.5
        store["name"] = "bob"
        store["flag"] = true

        assertThat(store.toJson(), equalTo("""{"count":2,"ratio":2.5,"name":"bob","flag":true}"""))
    }

    @Test
    fun `transport json omits local signals at any depth`() {
        store["count"] = 1.0
        store["_local"] = "secret"
        store["user.name"] = "bob"
        store["user._token"] = "abc"

        assertThat(store.toTransportJson(), equalTo("""{"count":1,"user":{"name":"bob"}}"""))
    }

    @Test
    fun `escapes strings in json`() {
        store["text"] = "a\"b\\c\nd"
        assertThat(store.toJson(), equalTo("""{"text":"a\"b\\c\nd"}"""))
    }
}
