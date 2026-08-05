package org.http4k.connect.amazon.dynamodb.grammar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
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

    // the only form real DynamoDB accepts - the bare name above is a fake-only leniency
    @Test
    fun `attribute type - type as an expression attribute value`() {
        val item = Item(attr1 of "123")

        assertTrue("attribute_type(attr1, :type)", item, mapOf(":type" to AttributeValue.Str("S")))
        assertFalse("attribute_type(attr1, :type)", item, mapOf(":type" to AttributeValue.Str("SS")))

        assertTrue(
            "attribute_type(#key1, :type)", item,
            mapOf(":type" to AttributeValue.Str("S")),
            mapOf("#key1" to attr1.name)
        )
    }

    @Test
    fun `attribute type - NULL as an expression attribute value`() {
        val nullType = mapOf(":type" to AttributeValue.Str("NULL"))

        assertTrue("attribute_type(attr1, :type)", mapOf(attr1.name to AttributeValue.Null()), nullType)
        assertFalse("attribute_type(attr1, :type)", Item(attr1 of "123"), nullType)
    }

    @Test
    fun `attribute type - undefined expression attribute value`() {
        val error = assertThrows<DynamoDbConditionError> {
            DynamoDbConditionalGrammar.parse("attribute_type(attr1, :type)").eval(ItemWithSubstitutions(Item()))
        }

        assertThat(
            error.message,
            equalTo("An expression attribute value used in expression is not defined; attribute value: :type")
        )
    }

    @Test
    fun `attribute type - expression attribute value which is not a type name`() {
        val validTypes = "valid types: {B,BOOL,BS,L,M,N,NS,NULL,S,SS}"

        assertThat(
            attributeTypeError(AttributeValue.Str("banana")).message,
            equalTo("Invalid attribute type name found; type: banana, $validTypes")
        )
        assertThat(
            attributeTypeError(AttributeValue.Num(123)).message,
            equalTo("Invalid attribute type name found; type: AttributeValue(N=123), $validTypes")
        )
    }

    @Test
    fun `attribute type - bare name which is not a type name`() {
        val error = assertThrows<DynamoDbConditionError> {
            DynamoDbConditionalGrammar.parse("attribute_type(attr1, banana)").eval(ItemWithSubstitutions(Item()))
        }

        assertThat(
            error.message,
            equalTo("Invalid attribute type name found; type: banana, valid types: {B,BOOL,BS,L,M,N,NS,NULL,S,SS}")
        )
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

    /**
     * The left operand is true, so `eval` would short-circuit before reaching the branch under test.
     * The message is asserted as well as the type, so a node whose own [Expr.validate] is missing cannot
     * pass on an error raised somewhere else in the expression.
     *
     * The indexed and map rows report a reserved word rather than an undefined `#name`: the grammar
     * cannot parse `#name` as either the target of an index or the child of a map path.
     */
    @TestFactory
    fun `validation reaches unreached short-circuited branch per node kind`() = listOf(
        Triple("equal", "attr1 = :missing", undefinedValue),
        Triple("not equal", "attr1 <> :missing", undefinedValue),
        Triple("greater than", "attrNum > :missing", undefinedValue),
        Triple("greater than or equal", "attrNum >= :missing", undefinedValue),
        Triple("less than", "attrNum < :missing", undefinedValue),
        Triple("less than or equal", "attrNum <= :missing", undefinedValue),
        Triple("begins with", "begins_with(attr1, :missing)", undefinedValue),
        Triple("contains", "contains(attr1, :missing)", undefinedValue),
        Triple("between", "attrNum BETWEEN :missing AND :max", undefinedValue),
        Triple("in", "attr1 IN (:missing, :known)", undefinedValue),
        Triple("and", "attr1 = :known AND #missing = :known", undefinedName),
        Triple("not", "NOT #missing = :known", undefinedName),
        Triple("size", "size(#missing) = :length", undefinedName),
        Triple("paren", "(#missing = :known)", undefinedName),
        Triple("attribute exists", "attribute_exists(#missing)", undefinedName),
        Triple("attribute not exists", "attribute_not_exists(#missing)", undefinedName),
        Triple("attribute type", "attribute_type(#missing, :type)", undefinedName),
        Triple("indexed attribute value", "MISSING[0] = :known", reservedWord),
        Triple("map attribute value", "attrMap.MISSING = :known", reservedWord)
    ).map { (node, branch, expected) ->
        DynamicTest.dynamicTest(node) {
            val error = assertThrows<DynamoDbConditionError> {
                DynamoDbConditionalGrammar.parse("attribute_not_exists(absent) OR ($branch)")
                    .validate(
                        ItemWithSubstitutions(
                            Item(attr1 of "123", attrNum of 5, attrMap of Item(attr1 of "123")),
                            values = mapOf(
                                ":known" to attr1.asValue("123"),
                                ":length" to attrNum.asValue(3),
                                ":max" to attrNum.asValue(10),
                                ":type" to AttributeValue.Str("S")
                            )
                        )
                    )
            }

            assertThat(error.message, equalTo(expected))
        }
    }

    private val undefinedValue =
        "An expression attribute value used in expression is not defined; attribute value: :missing"
    private val undefinedName =
        "An expression attribute name used in the document path is not defined; attribute name: #missing"
    private val reservedWord = "Attribute name is a reserved keyword; reserved keyword: MISSING"

    private fun attributeTypeError(type: AttributeValue) = assertThrows<DynamoDbConditionError> {
        DynamoDbConditionalGrammar.parse("attribute_type(attr1, :type)")
            .eval(ItemWithSubstitutions(Item(), values = mapOf(":type" to type)))
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
