package org.http4k.connect.amazon.dynamodb.grammar


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.attrBool
import org.http4k.connect.amazon.dynamodb.attrL
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrNS
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.attrSS
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import org.junit.jupiter.api.Test

class DynamoDbUpdateGrammarTest {

    @Test
    fun `remove - exists`() = assert(
        expression = "REMOVE $attrN",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a")
    )

    @Test
    fun `remove - missing`() = assert(
        expression = "REMOVE $attrN",
        item = Item(attrS of "a"),
        expected = Item(attrS of "a")
    )

    @Test
    fun `remove - named`() = assert(
        expression = "REMOVE #key1",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a"),
        names = mapOf("#key1" to attrN.name)
    )

    @Test
    fun `remove - from list, middle`() = assert(
        expression = "REMOVE $attrL[1]",
        item = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2), attrN.asValue(3))),
        expected = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(3))),
    )

    @Test
    fun `remove - from list, start`() = assert(
        expression = "REMOVE $attrL[0]",
        item = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2), attrN.asValue(3))),
        expected = Item(attrL of listOf(attrN.asValue(2), attrN.asValue(3))),
    )

    @Test
    fun `remove - from list, end`() = assert(
        expression = "REMOVE $attrL[2]",
        item = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2), attrN.asValue(3))),
        expected = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2))),
    )

    @Test
    fun `remove - two actions`() {
        val item = Item(attrS of "a", attrN of 1, attrSS of setOf("b"))

        assert("REMOVE $attrN $attrSS", item, Item(attrS of "a"))
    }

    @Test
    fun `set - missing attribute`() = assert(
        expression = "SET $attrN = :val1",
        item = Item(attrS of "a"),
        expected = Item(attrS of "a", attrN of 1),
        values = mapOf(":val1" to attrN.asValue(1))
    )

    @Test
    fun `set - existing attribute`() = assert(
        expression = "SET $attrN = :val1",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a", attrN of 2),
        values = mapOf(":val1" to attrN.asValue(2))
    )

    @Test
    fun `set - to self, named`() = assert(
        expression = "SET #key1 = #key1",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a", attrN of 1),
        names = mapOf("#key1" to attrN.name),
        values = mapOf(":val1" to attrN.asValue(1))
    )

    @Test
    fun `set - plus value`() = assert(
        expression = "SET $attrN = $attrN + :val1",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a", attrN of 2),
        values = mapOf(":val1" to attrN.asValue(1))
    )

    @Test
    fun `set - plus item value`() = assert(
        expression = "SET $attrN = $attrN + #key1",
        item = Item(attrS of "a", attrN of 4),
        expected = Item(attrS of "a", attrN of 8),
        names = mapOf("#key1" to attrN.name)
    )

    @Test
    fun `set - minus value`() = assert(
        expression = "SET $attrN = $attrN - :val1",
        item = Item(attrS of "a", attrN of 2),
        expected = Item(attrS of "a", attrN of 1),
        values = mapOf(":val1" to attrN.asValue(1))
    )

    @Test
    fun `set - minus item value`() = assert(
        expression = "SET $attrN = $attrN - #key1",
        item = Item(attrS of "a", attrN of 4),
        expected = Item(attrS of "a", attrN of 0),
        names = mapOf("#key1" to attrN.name)
    )

    @Test
    fun `set - multiple`() = assert(
        expression = "SET $attrN = :val1, $attrBool = :val2",
        item = Item(attrS of "a", attrN of 4),
        expected = Item(attrS of "a", attrN of 2, attrBool of true),
        values = mapOf(":val1" to attrN.asValue(2), ":val2" to attrBool.asValue(true))
    )

    @Test
    fun `set - list_append`() = assert(
        expression = "SET #key1 = list_append(#key1, :val1)",
        item = Item(attrL of listOf(attrN.asValue(1))),
        expected = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2))),
        names = mapOf("#key1" to attrL.name),
        values = mapOf(":val1" to attrL.asValue(listOf(attrN.asValue(2))))
    )

    @Test
    fun `set - if_not_exists - exists`() = assert(
        expression = "SET #key1 = if_not_exists(#key1, :val1)",
        item = Item(attrN of 1),
        expected = Item(attrN of 1),
        names = mapOf("#key1" to attrN.name),
        values = mapOf(":val1" to attrN.asValue(2))
    )

    @Test
    fun `set - if_not_exists - not exists`() = assert(
        expression = "SET #key1 = if_not_exists(#key1, :val1)",
        item = Item(),
        expected = Item(attrN of 2),
        names = mapOf("#key1" to attrN.name),
        values = mapOf(":val1" to attrN.asValue(2))
    )

    @Test
    fun `set - element of list`() = assert(
        expression = "SET $attrL[1] = :val1",
        item = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(2))),
        expected = Item(attrL of listOf(attrN.asValue(1), attrN.asValue(4))),
        values = mapOf(":val1" to attrN.asValue(4))
    )

    @Test
    fun `add - multiple`() = assert(
        expression = "SET #key1 = :val1 REMOVE #key2",
        item = Item(attrS of "a", attrN of 1),
        expected = Item(attrS of "a", attrBool of true),
        names = mapOf("#key1" to attrBool.name, "#key2" to attrN.name),
        values = mapOf(":val1" to attrBool.asValue(true))
    )

    @Test
    fun `add - number`() = assert(
        expression = "ADD $attrN :val1",
        item = Item(attrN of 1),
        values = mapOf(":val1" to attrN.asValue(2)),
        expected = Item(attrN of 3)
    )

    @Test
    fun `add - set`() = assert(
        expression = "ADD #key1 :val1",
        item = Item(attrSS of setOf("foo")),
        names = mapOf("#key1" to attrSS.name),
        values = mapOf(":val1" to attrSS.asValue(setOf("bar", "baz"))),
        expected = Item(attrSS of setOf("foo", "bar", "baz"))
    )

    @Test
    fun `add - missing from item`() = assert(
        expression = "ADD #key1 :val1",
        item = Item(),
        names = mapOf("#key1" to attrSS.name),
        values = mapOf(":val1" to attrSS.asValue(setOf("bar", "baz"))),
        expected = Item(attrSS of setOf("bar", "baz"))
    )

    @Test
    fun `add - two actions`() = assert(
        expression = "ADD $attrN :val1, #key1 :val2",
        item = Item(attrN of 1, attrNS of setOf(1, 2)),
        expected = Item(attrN of 3, attrNS of setOf(1, 2, 3)),
        names = mapOf("#key1" to attrNS.name),
        values = mapOf(":val1" to attrN.asValue(2), ":val2" to attrNS.asValue(setOf(3)))
    )

    @Test
    fun `delete - from set`() = assert(
        expression = "DELETE #key1 :val1",
        item = Item(attrSS of setOf("A", "B")),
        expected = Item(attrSS of setOf("B")),
        names = mapOf("#key1" to attrSS.name),
        values = mapOf(":val1" to attrSS.asValue(setOf("A")))
    )

    @Test
    fun `delete - multiple`() = assert(
        expression = "DELETE #key1 :val1, $attrNS :val2",
        item = Item(attrSS of setOf("A", "B"), attrNS of setOf(1, 2)),
        expected = Item(attrSS of setOf("B"), attrNS of setOf(1)),
        names = mapOf("#key1" to attrSS.name),
        values = mapOf(":val1" to attrSS.asValue(setOf("A")), ":val2" to attrNS.asValue(setOf(2)))
    )

    @Test
    fun `delete - missing from set`() = assert(
        expression = "DELETE #key1 :val1",
        item = Item(attrSS of setOf("A", "B")),
        expected = Item(attrSS of setOf("A", "B")),
        names = mapOf("#key1" to attrSS.name),
        values = mapOf(":val1" to attrSS.asValue(setOf("C")))
    )
}

private fun assert(
    expression: String,
    item: Item,
    expected: Item,
    values: TokensToValues = emptyMap(),
    names: TokensToNames = emptyMap()
) {
    assert(expression, ItemWithSubstitutions(item, names, values), expected)
}

private fun assert(
    expression: String,
    item: ItemWithSubstitutions,
    expected: Item
) {
    val dynamoDbGrammar = DynamoDbUpdateGrammar.parse(expression)
    assertThat(
        "\nexpression=$expression\nitem=${item.item}\nvalues=${item.values}\nnames=${item.names}\n",
        dynamoDbGrammar.eval(item), equalTo(expected)
    )
}
