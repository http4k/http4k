package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.junit.jupiter.api.Test

class HelpersKtTest {
    private val attrNum = Attribute.int().required("attrNum")
    private val attrStr = Attribute.string().required("attrStr")
    private val attrBoolean = Attribute.boolean().required("attrBool")
    private val attrList = Attribute.list().required("attrList")
    private val attrMap = Attribute.map().required("attrMap")

    private val item = Item(
        attrNum of 123,
        attrStr of "hello",
        attrList of listOf(
            attrNum.asValue(456),
            attrList.asValue(listOf(attrStr.asValue("there"), attrBoolean.asValue(true)))
        ),
        attrMap of Item(attrNum of 456, attrStr of "there", attrBoolean of true)
    )

    @Test
    fun `condition an item`() {
        assertThat(item.condition(null, emptyMap(), emptyMap()), equalTo(item))

        assertThat(
            item.condition(
                "attribute_exists(attrNum) AND #string = :value",
                mapOf("#string" to AttributeName.of("attrStr")),
                mapOf(":value" to attrStr.asValue("hello"))
            ), equalTo(item)
        )

        assertThat(
            item.condition(
                "attribute_exists(attrNum) AND #string = :value",
                mapOf("#string" to AttributeName.of("attrStr")),
                mapOf(":value" to attrStr.asValue("notHello"))
            ), absent()
        )
    }

    @Test
    fun `project from an item`() {
        assertThat(
            item.project(
                "#alias, attrList[0], attrMap.attrStr, attrList[1][1], attrMap.attrBool",
                mapOf("#alias" to AttributeName.of("attrNum"))
            ),
            equalTo(
                Item(
                    attrNum of 123,
                    attrList of listOf(attrNum.asValue(456), attrList.asValue(listOf(attrBoolean.asValue(true)))),
                    attrMap of Item(attrStr of "there", attrBoolean of true)
                )
            )
        )

        assertThat(item.project(null, emptyMap()), equalTo(item))
    }
}
