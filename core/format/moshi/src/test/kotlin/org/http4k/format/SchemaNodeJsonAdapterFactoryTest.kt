package org.http4k.format

import org.http4k.contract.jsonschema.SchemaNode
import org.http4k.contract.jsonschema.SchemaNode.Companion.Primitive
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.Moshi.asFormatString
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class SchemaNodeJsonAdapterFactoryTest {
    private val metadata = FieldMetadata(mapOf("foo" to "bar"))

    @Test
    fun reference(approver: Approver) {
        approver.assertApproved(
            SchemaNode.Reference(
                "name", "reffed", Primitive("reffed", IntegerParam, false, "foo", metadata),
                metadata
            )
        )
    }

    private fun Approver.assertApproved(input: SchemaNode) = assertApproved(asFormatString(input), APPLICATION_JSON)
}
