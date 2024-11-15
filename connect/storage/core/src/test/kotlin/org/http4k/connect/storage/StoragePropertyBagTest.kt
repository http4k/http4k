package org.http4k.connect.storage

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KMutableProperty0

class StoragePropertyBagTest {

    data class MyDataClass(val name: String)

    class MyType private constructor(value: Int) : IntValue(value) {
        companion object : IntValueFactory<MyType>(::MyType)
    }

    class Parent(storage: Storage<String>) : StoragePropertyBag(storage) {
        var standardField = "foobar"
        var string by item<String>()
        var nullableLong by item<Long?>()
        var value by item<MyType>()
        var child by item<MyDataClass>()
        var defaulted by item(MyDataClass("hello"))
    }

    @Test
    fun `set and get object values`() {
        val storage = Storage.InMemory<String>()
        val parent = Parent(storage)

        assertThrows<NoSuchElementException> { parent.string }
        assertThat(parent.standardField, equalTo("foobar"))
        assertThat(parent.nullableLong, absent())
        assertThat(parent.defaulted, equalTo(MyDataClass("hello")))

        expectSetWorks(parent::string, "helloworld")
        expectSetWorks(parent::nullableLong, 123L)
        expectSetWorks(parent::value, MyType.of(123))
        expectSetWorks(parent::child, MyDataClass("helloworld"))
        expectSetWorks(parent::defaulted, MyDataClass("goodbye"))

        assertThat(storage.keySet(), equalTo(setOf("string", "value", "defaulted", "nullableLong", "child")))

        expectSetWorks(parent::nullableLong, null)

        assertThat(storage.keySet(), equalTo(setOf("string", "value", "defaulted", "child")))
    }

    private fun <T> expectSetWorks(prop: KMutableProperty0<T>, value: T) {
        prop.set(value)
        assertThat(prop.get(), equalTo(value))
    }
}
