package org.http4k.contract.jsonschema.v2

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.contract.jsonschema.v2.JsonToJsonSchema
import org.http4k.lens.Header
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(JsonApprovalTest::class)
class JsonToJsonSchemaTest {
    private val json = org.http4k.format.Jackson

    private val creator = JsonToJsonSchema(json)

    @Test
    fun `renders object contents of different types of json value as expected`(approver: Approver) {

        approver.assertApproved(json {
            obj(
                "aString" to string("aStringValue"),
                "aNumber" to number(BigDecimal("1.9")),
                "aDouble" to number(1.01),
                "aBooleanTrue" to boolean(true),
                "aBooleanFalse" to boolean(false),
                "anArray" to array(listOf(obj("anotherString" to string("yetAnotherString")))),
                "anObject" to obj("anInteger" to number(1)),
                "anotherObject" to obj("anInteger" to number(1))
            )
        }, "bob")
    }

    @Test
    fun `renders array contents of different types of json value as expected`(approver: Approver) {
        approver.assertApproved(json {
            array(listOf(obj("anotherString" to string("yetAnotherString"))))
        }, "bob")
    }

    @Test
    fun `renders nested array contents of different types of json value as expected`(approver: Approver) {
        approver.assertApproved(json {
            array(listOf(array(listOf(obj("anotherString" to string("yetAnotherString"))))))
        }, "bob")
    }

    @Test
    fun `can provide prefix`(approver: Approver) {
        approver.assertApproved(json {
            array(listOf(obj("anotherString" to string("yetAnotherString"))))
        }, "bob", "prefix")
    }

        private fun Approver.assertApproved(obj: com.fasterxml.jackson.databind.JsonNode, name: String, prefix: String? = null) {
        assertApproved(
            Response(Status.OK)
            .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(org.http4k.format.Jackson.asFormatString(creator.toSchema(obj, name, prefix))))
    }
}
