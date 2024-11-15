package org.http4k.connect.example

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.example.action.Echoed
import org.http4k.connect.example.action.Reversed
import org.http4k.connect.example.action.doubleReverse
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.Test

interface ExampleContract {
    val http: HttpHandler

    @Test
    fun `can echo`() {
        assertThat(Example.Http(http).echo("hello"), equalTo(Success(Echoed("hello"))))
    }

    @Test
    fun `can reverse`() {
        assertThat(Example.Http(http).reverse("hello"), equalTo(Success(Reversed("olleh"))))
    }

    @Test
    fun `can double reverse`() {
        assertThat(Example.Http(http).doubleReverse("hello"), equalTo(Success(Reversed("hello"))))
    }

    @Test
    fun `can split`() {
        assertThat(Example.Http(http).splitPaginated("splitme").toList(), equalTo(listOf(Success("splitme".toList()))))
    }
}
