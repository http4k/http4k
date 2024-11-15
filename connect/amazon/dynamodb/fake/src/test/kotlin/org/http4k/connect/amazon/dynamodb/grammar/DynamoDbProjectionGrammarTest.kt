package org.http4k.connect.amazon.dynamodb.grammar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.junit.jupiter.api.Test

class DynamoDbProjectionGrammarTest {

    private val attrNum = Attribute.int().required("attrNum")
    private val attr1 = Attribute.string().required("attr1")
    private val attrList = Attribute.list().required("attrList")
    private val attrMap = Attribute.map().required("attrMap")

    @Test
    fun `attribute value`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("attr1").eval(ItemWithSubstitutions(Item(attr1 of "123"))),
            equalTo(listOf(attr1.name to attr1.asValue("123")))
        )
    }

    @Test
    fun `attribute value with expression name`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("#att")
                .eval(ItemWithSubstitutions(Item(attr1 of "123"), mapOf("#att" to AttributeName.of("attr1")))),
            equalTo(listOf(attr1.name to attr1.asValue("123")))
        )
    }

    @Test
    fun `indexed attribute value`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("attrList[1]").eval(
                ItemWithSubstitutions(
                    Item(
                        attr1 of "DON'T GET ME",
                        attrList of listOf(
                            attr1.asValue("123"),
                            attrList.asValue(
                                listOf(attrNum.asValue(456))
                            )
                        )
                    )
                )
            ),
            equalTo(listOf(attrList.name to attrList.asValue(listOf(attrList.asValue(listOf(attrNum.asValue(456)))))))
        )
    }

    @Test
    fun `indexed attribute value with expression name`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("#theList[1]").eval(
                ItemWithSubstitutions(
                    Item(
                        attr1 of "DON'T GET ME",
                        attrList of listOf(
                            attr1.asValue("123"),
                            attrList.asValue(
                                listOf(attrNum.asValue(456))
                            )
                        )
                    ),
                    mapOf("#theList" to AttributeName.of("attrList"))
                )
            ),
            equalTo(listOf(attrList.name to attrList.asValue(listOf(attrList.asValue(listOf(attrNum.asValue(456)))))))
        )
    }

    @Test
    fun `multiple indexed attribute value`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("attrList[0][1][2]").eval(
                ItemWithSubstitutions(
                    Item(
                        attrList of listOf(
                            attrList.asValue(
                                listOf(
                                    attr1.asValue("111"),
                                    attrList.asValue(
                                        listOf(
                                            attr1.asValue("222"),
                                            attr1.asValue("333"),
                                            attrNum.asValue(444)
                                        )
                                    ),
                                    attrNum.asValue(555),
                                )
                            ),
                            attr1.asValue("666"),
                        ),
                        attr1 of "777"
                    ),
                )
            ),
            equalTo(
                listOf(
                    attrList.name to attrList.asValue(
                        listOf(
                            attrList.asValue(
                                listOf(
                                    attrList.asValue(
                                        listOf(
                                            attrNum.asValue(444)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `map attribute value`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("attrMap.attr1").eval(
                ItemWithSubstitutions(Item(attrMap of Item(attr1 of "456", attrNum of 456)))
            ),
            equalTo(listOf(attrMap.name to attrMap.asValue(Item(attr1 of "456"))))
        )
    }

    @Test
    fun `map attribute value with expression name`() {
        assertThat(
            DynamoDbProjectionGrammar.parse("#map.#att").eval(
                ItemWithSubstitutions(
                    Item(attrMap of Item(attr1 of "456", attrNum of 456)),
                    mapOf("#map" to AttributeName.of("attrMap"), "#att" to AttributeName.of("attr1"))
                )
            ),
            equalTo(listOf(attrMap.name to attrMap.asValue(Item(attr1 of "456"))))
        )
    }
}
