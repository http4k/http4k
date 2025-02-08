package dev.forkhandles.data

import dev.forkhandles.data.ContainerMeta.bar
import dev.forkhandles.data.ContainerMeta.foo
import dev.forkhandles.values.BooleanValue
import dev.forkhandles.values.BooleanValueFactory
import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.LocalDateValue
import dev.forkhandles.values.LocalDateValueFactory
import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.minValue
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.message
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDate.EPOCH
import java.time.LocalDate.MAX
import java.time.LocalDate.MIN
import java.time.LocalDate.of
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.full.starProjectedType

interface MainClassFields<C : ChildFields<G>, G : GrandchildFields, CONTENT> {

    var standardField: String

    var string: String
    var boolean: Boolean
    var int: Int
    var long: Long
    var double: Double
    var decimal: BigDecimal
    var bigInt: BigInteger
    var notAString: String

    var mapped: Int

    var list: List<String>
    var listSubClass: List<C>
    var listInts: List<Int>
    var listValue: List<LocalDateType>
    val listMapped: List<String>

    var subClass: C

    var value: MyType

    var optional: String?
    var optionalMapped: Int?
    val optionalValue: MyType?
    var optionalSubClass: C?
    var optionalSubClassList: List<C>?
    var optionalList: List<String>?
    var optionalValueList: List<MyType>?
    var optionalMappedList: List<Int>?
    var optionalData: CONTENT?
    var requiredData: CONTENT

    var longValue: LongType?
    var booleanValue: BooleanType
    var stringValue: StringType
    var localDateValue: LocalDateType
}

class LongType private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<LongType>(::LongType, 0L.minValue)
}

class BooleanType private constructor(value: Boolean) : BooleanValue(value) {
    companion object : BooleanValueFactory<BooleanType>(::BooleanType)
}

class LocalDateType private constructor(value: LocalDate) : LocalDateValue(value) {
    companion object : LocalDateValueFactory<LocalDateType>(::LocalDateType)
}

class StringType private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<StringType>(::StringType)
}

enum class ContainerMeta : Metadatum {
    foo, bar
}

interface GrandchildFields {
    var long: Long
}

interface ChildFields<T : GrandchildFields> {
    var string: String
    var noSuch: String
    var grandchild: T
}

class MyType private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MyType>(::MyType)
}

@ExtendWith(ApprovalTest::class)
abstract class DataContainerContract<C : ChildFields<G>, G : GrandchildFields, CONTENT> {

    abstract fun data(input: Map<String, Any?>): CONTENT
    abstract fun container(input: Map<String, Any?>): MainClassFields<C, G, CONTENT>
    abstract fun childContainer(input: Map<String, Any?>): C
    abstract fun grandchildContainer(input: Map<String, Any?>): G

    @Test
    fun `can read primitives values`() {
        val input = container(
            mapOf(
                "string" to "string",
                "boolean" to true,
                "int" to 123,
                "long" to Long.MAX_VALUE,
                "double" to 1.1234,
                "decimal" to "1.1234",
                "notAString" to 123,
                "value" to 123,
                "mapped" to "123",
                "optionalValue" to 123,
                "optional" to "optional",
                "stringValue" to "stringValue",
                "booleanValue" to true,
                "localDateValue" to "1999-12-31",
                "longValue" to 1,
            )
        )

        expectThat(input.standardField).isEqualTo("foobar")

        expectThat(input.string).isEqualTo("string")
        expectThrows<NoSuchElementException> { container(mapOf()).string }.message.isEqualTo("Field <string> is missing")
        expectThrows<NoSuchElementException> { input.notAString }.message.isEqualTo("Value for field <notAString> is not a class kotlin.String but class kotlin.Int")

        expectThat(input.boolean).isEqualTo(true)
        expectThat(input.int).isEqualTo(123)
        expectThat(input.long).isEqualTo(Long.MAX_VALUE)
        expectThat(input.double).isEqualTo(1.1234)

        expectThat(input.mapped).isEqualTo(123)
        expectThrows<ClassCastException> { container(mapOf("mapped" to 123)).mapped }
        expectThat(input.value).isEqualTo(MyType.of(123))
        expectThat(input.stringValue).isEqualTo(StringType.of("stringValue"))
        expectThat(input.longValue).isEqualTo(LongType.of(1))
        expectThat(input.localDateValue).isEqualTo(LocalDateType.of(of(1999, 12, 31)))
        expectThat(input.booleanValue).isEqualTo(BooleanType.of(true))

        expectThat(input.optional).isEqualTo("optional")
        expectThat(container(mapOf()).optional).isNull()

        expectThat(input.optionalValue).isEqualTo(MyType.of(123))
        expectThat(container(mapOf()).optionalValue).isNull()
    }

    @Test
    fun `can write primitives values`(approver: Approver) {
        val input = container(
            mapOf(
                "string" to "string",
                "boolean" to true,
                "int" to 123,
                "long" to Long.MAX_VALUE,
                "double" to 1.1234,
                "value" to 123,
                "mapped" to "123",

                "optionalValue" to 123,
                "optional" to "optional",
                "localDateValue" to "1999-12-31",
                "stringValue" to "stringValue",
                "booleanValue" to true,
                "longValue" to 1
            )
        )

        expectSetWorks(input::standardField, "123")
        expectSetWorks(input::string, "123")
        expectSetWorks(input::boolean, false)
        expectSetWorks(input::int, 999)
        expectSetWorks(input::long, 0)
        expectSetWorks(input::double, 5.4536)
        expectSetWorks(input::stringValue, StringType.of("123"))
        expectSetWorks(input::longValue, LongType.of(123))
        expectSetWorks(input::localDateValue, LocalDateType.of(of(1999, 12, 12)))
        expectSetWorks(input::booleanValue, BooleanType.of(false))

        expectSetWorks(input::optional, "123123")
        expectSetWorks(input::optional, null)
        expectSetWorks(input::mapped, 123)

        approver.assertApproved(input.toString())
    }

    @Test
    fun `read object values`() {
        val input = container(
            mapOf(
                "subClass" to mapOf(
                    "string" to "string"
                ),
                "optionalSubClass" to mapOf(
                    "string" to "string"
                )
            )
        )

        expectThat(input.subClass.string).isEqualTo("string")
        expectThrows<NoSuchElementException> { input.subClass.noSuch }.message.isEqualTo("Field <noSuch> is missing")

        expectThat(input.optionalSubClass?.string).isEqualTo("string")
        expectThat(container(mapOf()).optionalSubClass).isNull()
        expectThat(input.optionalSubClass?.string).isEqualTo("string")
    }

    @Test
    fun `read and write data values`(approver: Approver) {
        val inner = data(mutableMapOf("name" to "string"))
        val outer = container(mapOf("requiredData" to inner))

        expectThat(outer.requiredData).isEqualTo(inner)
        expectThat(outer.optionalData).isNull()
        outer.optionalData = inner
        expectThat(outer.optionalData).isEqualTo(inner)

        approver.assertApproved(inner.toString())
    }

    @Test
    fun `write object values`(approver: Approver) {
        val grandchild = mapOf(
            "long" to 1234
        )
        val child = mapOf(
            "string" to "string2"
        )
        val top = container(
            mapOf(
                "object" to child,
                "optionalObject" to mapOf(
                    "string" to "string"
                )
            )
        )

        val childObj = childContainer(child)
        expectSetWorks(top::subClass, childObj)
        expectThat(top.subClass).isEqualTo(childContainer(child))

        val gcObj = grandchildContainer(grandchild)
        expectSetWorks(childObj::grandchild, gcObj)
        expectThat(childObj.grandchild).isEqualTo(grandchildContainer(grandchild))

        expectSetWorks(top::optionalSubClass, childObj)
        expectThat(top.optionalSubClass).isEqualTo(childObj)
        expectSetWorks(top::optionalSubClass, null)
        expectThat(top.optionalSubClass).isEqualTo(null)

        approver.assertApproved((top as DataContainer<*>).toString())
    }

    @Test
    fun `read list values`() {
        val input = container(
            mapOf(
                "list" to listOf("string1", "string2"),
                "listInts" to listOf(1, 2, 3),
                "listMapped" to listOf(123, 456),
                "listValue" to listOf(MAX, MIN, EPOCH).map { it.toString() },
                "listSubClass" to listOf(
                    mapOf("string" to "string1"),
                    mapOf("string" to "string2"),
                ),
                "optionalList" to listOf("hello")
            )
        )
        expectThat(input.list).isEqualTo(listOf("string1", "string2"))
        expectThat(input.listMapped).isEqualTo(listOf("123", "456"))
        expectThat(input.listInts).isEqualTo(listOf(1, 2, 3))
        expectThat(input.listValue).isEqualTo(listOf(MAX, MIN, EPOCH).map(LocalDateType::of))
        expectThat(input.listSubClass.map { it.string }).isEqualTo(listOf("string1", "string2"))

        expectThat(input.optionalList).isEqualTo(listOf("hello"))
        expectThat(container(mapOf()).optionalList).isNull()
    }

    @Test
    fun `write list values`(approver: Approver) {
        val input = container(
            mapOf(
                "list" to listOf("string1", "string2"),
                "listSubClass" to listOf(
                    mapOf("string" to "string1"),
                    mapOf("string" to "string2"),
                ),
                "listValue" to listOf(MAX, MIN, EPOCH).map { it.toString() },
                "optionalList" to listOf("hello")
            )
        )

        expectSetWorks(input::list, listOf("123"))
        expectSetWorks(input::listSubClass, listOf(childContainer(mapOf("123" to "123"))))
        expectSetWorks(input::listValue, listOf(LocalDateType.of(MAX), LocalDateType.of(MIN), LocalDateType.of(EPOCH)))
        expectSetWorks(input::optionalSubClassList, listOf(childContainer(mapOf("123" to "123"))))
        expectSetWorks(input::optionalValueList, listOf(MyType.of(123), MyType.of(456)))
        expectSetWorks(input::optionalList, listOf("hello"))

        approver.assertApproved(input.toString())
    }

    @Test
    fun `get meta data from the container`() {
        val input = container(emptyMap()) as DataContainer<*>

        val propertyMetaData = input.propertyMetadata().find { it.name == "string" }
        expectThat(propertyMetaData).isEqualTo(
            PropertyMetadata(
                "string",
                String::class.starProjectedType,
                listOf(foo, bar)
            )
        )
    }

    private fun <T> expectSetWorks(prop: KMutableProperty0<T>, value: T) {
        prop.set(value)
        expectThat(prop.get()).isEqualTo(value)
    }

    @Test
    abstract fun `can update an arbitrary value`(approver: Approver)
}
