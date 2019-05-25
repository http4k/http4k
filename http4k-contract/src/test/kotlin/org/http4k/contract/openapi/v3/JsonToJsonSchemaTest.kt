package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Header
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(JsonApprovalTest::class)
class JsonToJsonSchemaTest {
    private val json = Jackson

    private val creator = JsonToJsonSchema(json)

    @Test
    fun `renders object contents of different types of json value as expected`(approver: Approver) {

        approver.assertApproved(json {
            obj(
                "aString" to string("aStringValue"),
                "aNumber" to number(BigDecimal("1.9")),
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

    private fun Approver.assertApproved(obj: JsonNode, name: String) {
        assertApproved(Response(Status.OK)
            .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(Jackson.asJsonString(creator.toSchema(obj, name))))
    }
}
