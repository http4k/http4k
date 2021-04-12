package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.junit.jupiter.api.Test
import com.squareup.moshi.Moshi as SMoshi

data class Foo(val value: String)
data class Bar(val value: String)

object FooAdapter : JsonAdapter<Foo>() {
    override fun fromJson(p0: JsonReader) = Foo("hello")

    override fun toJson(p0: JsonWriter, p1: Foo?) {
        p0.value("hello")
    }
}

object BarAdapter : JsonAdapter<Bar>() {
    override fun fromJson(p0: JsonReader) = Bar("hello")

    override fun toJson(p0: JsonWriter, p1: Bar?) {
        p0.value("hello")
    }
}

class SimpleMoshiAdapterFactoryTest {
    @Test
    fun `register adapter`() {
        val moshi = SMoshi.Builder()
            .add(FooAdapter.asFactory())
            .add(SimpleMoshiAdapterFactory(adapter { BarAdapter }))
            .build()
        assertThat(moshi.adapter(Foo::class.java), equalTo(FooAdapter))
        assertThat(moshi.adapter(Bar::class.java), equalTo(BarAdapter))
    }
}
