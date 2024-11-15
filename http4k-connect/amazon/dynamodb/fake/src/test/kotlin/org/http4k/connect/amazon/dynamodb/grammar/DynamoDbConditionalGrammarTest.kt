package org.http4k.connect.amazon.dynamodb.grammar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import org.junit.jupiter.api.Test
import java.time.Duration.ofSeconds

class DynamoDbConditionalGrammarTest {

    private val attrNum = Attribute.int().required("attrNum")
    private val attr1 = Attribute.string().required("attr1")
    private val attrList = Attribute.list().required("attrList")
    private val attrMap = Attribute.map().required("attrMap")
    private val attr2 = Attribute.duration().required("attr2")
    private val attr3 = Attribute.strings().required("attr3")

    @Test
    fun `=`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 = :foo", item, mapOf(":foo" to attr1.asValue("123")))
        assertTrue("attr2 = :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(123))))

        assertFalse("attr1 = :foo", item, mapOf(":foo" to attr1.asValue("789")))
    }

    @Test
    fun `!=`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 <> :foo", item, mapOf(":foo" to attr1.asValue("789")))
        assertTrue("attr2 <> :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(456))))

        assertFalse("attr1 <> :foo", item, mapOf(":foo" to attr1.asValue("123")))
    }

    @Test
    fun `less than`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 < :foo", item, mapOf(":foo" to attr1.asValue("789")))
        assertTrue("attr2 < :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(456))))

        assertFalse("attr1 < :foo", item, mapOf(":foo" to attr1.asValue("123")))
    }

    @Test
    fun `less than or equal`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 <= :foo", item, mapOf(":foo" to attr1.asValue("123")))
        assertTrue("attr1 <= :foo", item, mapOf(":foo" to attr1.asValue("124")))
        assertTrue("attr2 <= :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(123))))
        assertTrue("attr2 <= :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(124))))

        assertFalse("attr1 <= :foo", item, mapOf(":foo" to attr1.asValue("122")))
    }

    @Test
    fun `greater than`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 > :foo", item, mapOf(":foo" to attr1.asValue("122")))
        assertTrue("attr2 > :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(122))))

        assertFalse("attr1 > :foo", item, mapOf(":foo" to attr1.asValue("123")))
    }

    @Test
    fun `greater than or equal`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("attr1 >= :foo", item, mapOf(":foo" to attr1.asValue("122")))
        assertTrue("attr1 >= :foo", item, mapOf(":foo" to attr1.asValue("122")))
        assertTrue("attr2 >= :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(122))))
        assertTrue("attr2 >= :foo", item, mapOf(":foo" to attr2.asValue(ofSeconds(123))))

        assertFalse("attr1 >= :foo", item, mapOf(":foo" to attr1.asValue("124")))
    }

    @Test
    fun `size of field`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("size(attr1) = :foo", item, mapOf(":foo" to attrNum.asValue(3)))
        assertFalse("size(attr1) = :foo", item, mapOf(":foo" to attrNum.asValue(4)))
    }

    @Test
    fun `attribute exists`() {
        val item = Item(attr1 of "123")
        assertTrue("attribute_exists(attr1)", item)
        assertFalse("attribute_exists(attr2)", item)
    }

    @Test
    fun `attribute exists - substitution of names`() {
        val item = Item(attr1 of "123")
        assertTrue("attribute_exists(#key1)", item, names = mapOf("#key1" to attr1.name))
        assertFalse("attribute_exists(#key1)", item, names = mapOf("#key1" to attr2.name))
    }

    @Test
    fun `attribute value`() {
        assertThat(
            DynamoDbConditionalGrammar.parse("attr1").eval(ItemWithSubstitutions(Item(attr1 of "123"))),
            equalTo(attr1.asValue("123"))
        )
    }

    @Test
    fun `indexed attribute value`() {
        assertThat(
            DynamoDbConditionalGrammar.parse("attrList[1]").eval(
                ItemWithSubstitutions(
                    Item(
                        attrList of listOf(
                            attr1.asValue("123"),
                            attrNum.asValue(456)
                        )
                    )
                )
            ),
            equalTo(attrList.asValue(listOf(attrNum.asValue(456))))
        )
    }

    @Test
    fun `deeply indexed attribute value`() {
        assertThat(
            DynamoDbConditionalGrammar.parse("attrList[0][1][2]").eval(
                ItemWithSubstitutions(
                    Item(
                        attrList of listOf(
                            attrList.asValue(
                                listOf(
                                    attr1.asValue("123"),
                                    attrList.asValue(
                                        listOf(
                                            attr1.asValue("123"),
                                            attr1.asValue("123"),
                                            attrNum.asValue(456)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            equalTo(attrList.asValue(listOf(attrNum.asValue(456))))
        )
    }

    @Test
    fun `nested map attribute value`() {
        assertThat(
            DynamoDbConditionalGrammar.parse("attrMap.attrMap.attr1").eval(
                ItemWithSubstitutions(Item(attrMap of Item(attrMap of Item(attr1 of "123", attrNum of 456))))
            ),
            equalTo(attr1.asValue("123"))
        )
    }

    @Test
    fun `map attribute value`() {
        assertThat(
            DynamoDbConditionalGrammar.parse("attrMap.attr1").eval(
                ItemWithSubstitutions(Item(attrMap of Item(attr1 of "123", attrNum of 456)))
            ),
            equalTo(attr1.asValue("123"))
        )
    }

    @Test
    fun `attribute type`() {
        val item = Item(attr1 of "123")
        assertTrue("attribute_type(attr1, S)", item)
        assertFalse("attribute_type(attr1, SS)", item)
    }

    @Test
    fun `between function`() {
        val item = Item(attrNum of 5)
        assertTrue(
            "attrNum BETWEEN :foo AND :bar", item,
            mapOf(":foo" to attrNum.asValue(1), ":bar" to attrNum.asValue(10))
        )
        assertFalse(
            "attrNum BETWEEN :foo AND :bar", item,
            mapOf(":foo" to attrNum.asValue(7), ":bar" to attrNum.asValue(10))
        )
    }

    @Test
    fun `between composed function`() {
        val item = Item(attrNum of 5)
        assertTrue(
            "attrNum <= :bar AND attrNum BETWEEN :foo AND :bar", item,
            mapOf(":foo" to attrNum.asValue(1), ":bar" to attrNum.asValue(10))
        )
        assertFalse(
            "attrNum > :bar AND attrNum BETWEEN :foo AND :bar", item,
            mapOf(":foo" to attrNum.asValue(7), ":bar" to attrNum.asValue(10))
        )
    }

    @Test
    fun `IN function`() {
        val item = Item(attr1 of "123", attr3 of setOf("123", "456"))

        assertTrue(
            "attr1 IN (:foo, :bar)", item,
            mapOf(":foo" to attr1.asValue("123"), ":bar" to attr1.asValue("457"))
        )
        assertTrue(
            "attr1 IN (:bar, :foo)", item,
            mapOf(":foo" to attr1.asValue("123"), ":bar" to attr1.asValue("457"))
        )
        assertFalse(
            "attr1 IN (:bar, :bar)", item,
            mapOf(":bar" to attr1.asValue("457"))
        )
    }

    @Test
    fun `attribute not exists`() {
        val item = Item(attr1 of "123")
        assertTrue("attribute_not_exists(attr2)", item)
        assertFalse("attribute_not_exists(attr1)", item)
    }

    @Test
    fun `attributes not exists - substitution of names`() {
        val item = Item(attr1 of "123")
        assertTrue("attribute_not_exists(#key1)", item, names = mapOf("#key1" to attr2.name))
        assertFalse("attribute_not_exists(#key1)", item, names = mapOf("#key1" to attr1.name))
    }

    @Test
    fun `begins with`() {
        val item = Item(attr1 of "123")
        assertTrue("begins_with(attr1, :foo)", item, mapOf(":foo" to attr1.asValue("123")))
        assertFalse("begins_with(attr1, :foo)", item, mapOf(":foo" to attr1.asValue("124")))
    }

    @Test
    fun `contains function`() {
        val item = Item(attr1 of "123", attr3 of setOf("123", "456"))
        assertTrue("contains(attr1, :foo)", item, mapOf(":foo" to attr1.asValue("123")))
        assertTrue("contains(attr3, :foo)", item, mapOf(":foo" to attr1.asValue("123")))
        assertFalse("contains(attr1, :foo)", item, mapOf(":foo" to attr1.asValue("124")))
        assertFalse("contains(attr3, :foo)", item, mapOf(":foo" to attr1.asValue("124")))
        assertTrue("contains(attr1, :foo)", item, mapOf(":foo" to attr1.asValue("2")))
        assertFalse("contains(attr3, :foo)", item, mapOf(":foo" to attr1.asValue("2")))
    }

    @Test
    fun `logical NOT`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue("NOT attr1 = :foo", item, mapOf(":foo" to attr1.asValue("789")))

        assertFalse("NOT attr1 = :foo", item, mapOf(":foo" to attr1.asValue("123")))
    }

    @Test
    fun `logical AND`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue(
            "attr1 = :foo AND attr2 = :bar", item, mapOf(
                ":foo" to attr1.asValue("123"),
                ":bar" to attr2.asValue(ofSeconds(123))
            )
        )

        assertTrue(
            "attr1 = :foo AND (attr2 = :bar AND attr1 = :foo)", item, mapOf(
                ":foo" to attr1.asValue("123"),
                ":bar" to attr2.asValue(ofSeconds(123))
            )
        )

        assertFalse(
            "attr1 = :foo AND (attr2 = :bar AND attr1 = :bar)", item, mapOf(
                ":foo" to attr1.asValue("123"),
                ":bar" to attr2.asValue(ofSeconds(124))
            )
        )
    }

    @Test
    fun `logical OR`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue(
            "attr1 = :foo OR attr2 = :bar", item, mapOf(
                ":foo" to attr1.asValue("123"),
                ":bar" to attr2.asValue(ofSeconds(123))
            )
        )

        assertTrue(
            "attr1 = :foo OR attr2 = :bar", item, mapOf(
                ":foo" to attr1.asValue("123"),
                ":bar" to attr2.asValue(ofSeconds(124))
            )
        )

        assertTrue(
            "attr1 = :foo OR attr2 = :bar", item, mapOf(
                ":foo" to attr1.asValue("124"),
                ":bar" to attr2.asValue(ofSeconds(123))
            )
        )

        assertFalse(
            "attr1 = :foo OR attr2 = :bar", item, mapOf(
                ":foo" to attr1.asValue("124"),
                ":bar" to attr2.asValue(ofSeconds(124))
            )
        )
    }

    @Test
    fun `substitution of names`() {
        val item = Item(attr1 of "123", attr2 of ofSeconds(123))

        assertTrue(
            "#attr = :foo", item,
            mapOf(":foo" to attr1.asValue("123")),
            mapOf("#attr" to AttributeName.of("attr1")),
        )
    }

    private fun assertTrue(
        expression: String,
        item: Item,
        values: TokensToValues = emptyMap(),
        names: TokensToNames = emptyMap()
    ) {
        assert(expression, ItemWithSubstitutions(item, names, values), true)
    }

    private fun assertFalse(
        expression: String,
        item: Item,
        values: TokensToValues = emptyMap(),
        names: TokensToNames = emptyMap()
    ) {
        assert(expression, ItemWithSubstitutions(item, names, values), false)
    }

    private fun assert(
        expression: String,
        item: ItemWithSubstitutions,
        expected: Boolean
    ) {
        val dynamoDbGrammar = DynamoDbConditionalGrammar.parse(expression)
        assertThat(
            "$expression\n${item.item}\n${item.values}\n${item.names}",
            dynamoDbGrammar.eval(item), equalTo(expected)
        )
    }

}
