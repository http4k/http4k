package org.http4k.contract.jsonschema

import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.core.ContentType
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.ParamMeta
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
abstract class SchemaNodeMarshallingContract<NODE : Any>(private val json: AutoMarshallingJson<NODE>) {
    private val metadata = FieldMetadata(mapOf("foo" to "bar"))

    enum class TestEnum {
        A, B
    }

    @Test
    fun reference(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Reference(
                "name", "reffed", SchemaNode.Primitive("reffed", ParamMeta.IntegerParam, false, "foo", metadata),
                metadata
            )
        )
    }

    @Test
    fun enum(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Enum(
                "name", ParamMeta.StringParam, false, TestEnum.A, TestEnum.entries.map { it.name }, metadata
            )
        )
    }

    @Test
    fun primitive(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Primitive(
                "name", ParamMeta.StringParam, false, 123, metadata
            )
        )
    }

    @Test
    fun `an object`(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Object(
                "name", false, mapOf(
                    "node1" to SchemaNode.Primitive(
                        "name", ParamMeta.StringParam, false, 123, metadata
                    )
                ), mapOf<String, Any>(), metadata
            )
        )
    }

    @Test
    fun `array of refs`(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Array(
                "name", false, ArrayItem.Ref("reffed", listOf()), listOf("foo"), metadata
            )
        )
    }

    @Test
    fun `array of array of refs`(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Array(
                "name",
                false,
                ArrayItem.Array(ArrayItem.Ref("reffed", listOf()), "format", emptyList()),
                listOf("foo"),
                metadata
            )
        )
    }

    @Test
    fun `array of non objects`(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Array(
                "name",
                false,
                ArrayItem.NonObject(ParamMeta.StringParam, "format", emptyList()),
                listOf("asd"),
                metadata
            )
        )
    }

    private fun Approver.assertApproved(input: SchemaNode) {
        assertApproved(json.asFormatString(input), ContentType.APPLICATION_JSON)
    }
}
