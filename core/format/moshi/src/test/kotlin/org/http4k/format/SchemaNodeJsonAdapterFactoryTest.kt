package org.http4k.format

import org.http4k.contract.jsonschema.SchemaNode
import org.http4k.contract.jsonschema.SchemaNode.Companion.Enum
import org.http4k.contract.jsonschema.SchemaNode.Companion.Primitive
import org.http4k.contract.jsonschema.SchemaNode.Companion.Reference
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.Moshi.asFormatString
import org.http4k.format.SchemaNodeJsonAdapterFactoryTest.TestEnum.A
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class SchemaNodeJsonAdapterFactoryTest {
    private val metadata = FieldMetadata(mapOf("foo" to "bar"))

    enum class TestEnum {
        A, B
    }

    @Test
    fun reference(approver: Approver) {
        approver.assertApproved(
            Reference(
                "name", "reffed", Primitive("reffed", IntegerParam, false, "foo", metadata),
                metadata
            )
        )
    }

    @Test
    fun enum(approver: Approver) {
        approver.assertApproved(
            Enum(
                "name", ParamMeta.StringParam, false, A, TestEnum.entries.map { it.name }, metadata
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

    private fun Approver.assertApproved(input: SchemaNode) = assertApproved(asFormatString(input), APPLICATION_JSON)
}
