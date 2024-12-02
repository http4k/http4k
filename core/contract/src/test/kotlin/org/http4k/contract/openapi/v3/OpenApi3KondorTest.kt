package org.http4k.contract.openapi.v3

import com.ubertob.kondor.json.JAny
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.num
import com.ubertob.kondor.json.obj
import com.ubertob.kondor.json.str
import org.http4k.contract.ContractRendererContract
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.v3.KondorJsonSchemaCreator
import org.http4k.contract.meta
import org.http4k.contract.newContract
import org.http4k.contract.openapi.AddSimpleFieldToRootNode
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.format.KondorJson
import org.http4k.format.autoBody
import org.http4k.routing.experimental.newBind
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

private data class NestedObject(val aNullField: String?, val aNumberField: Int)
private data class TopLevelObject(val nestedObject: NestedObject)

private object JNestedObject : JAny<NestedObject>() {
    private val aNullField by str(NestedObject::aNullField)
    private val aNumberField by num(NestedObject::aNumberField)

    override fun JsonNodeObject.deserializeOrThrow(): NestedObject =
        NestedObject(
            aNullField = +aNullField,
            aNumberField = +aNumberField
        )
}

private object JTopLevelObject : JAny<TopLevelObject>() {
    private val nestedObject by obj(JNestedObject, TopLevelObject::nestedObject)

    override fun JsonNodeObject.deserializeOrThrow(): TopLevelObject =
        TopLevelObject(
            nestedObject = +nestedObject
        )
}

private val kondor = KondorJson {
    register(TopLevelObject::class, JTopLevelObject)
}

class OpenApi3KondorTest : ContractRendererContract<JsonNode>(
    kondor,
    OpenApi3(
        apiInfo = ApiInfo("title", "1.2", "module description"),
        json = kondor,
        apiRenderer = KondorOpenApi3Renderer(kondor, OpenApi3ApiRenderer(kondor)),
        servers = listOf(ApiServer(Uri.of("http://localhost:8000"))),
        extensions = listOf(AddSimpleFieldToRootNode),
    )
) {

    @Test
    override fun `renders as expected`(approver: Approver) {
        /*
            This scrubbing approver is necessary because the actual definition IDs being generated are not deterministic,
            as they're backed by data class hashcodes. In reality, this shouldn't be a problem for anyone using Kondor
            since it's expected that you'd define data classes for your data, rather than providing examples using
            arbitrary json.
         */
        val scrubbingApprover = object : Approver {
            override fun <T : HttpMessage> assertApproved(httpMessage: T) {
                val scrubbedBody = Regex("""/[a-zA-Z0-9_-]*(object-?\d+)"""").findAll(httpMessage.bodyString())
                    .map { it.groupValues[1] }
                    .toSet()
                    .foldIndexed(httpMessage.bodyString()) { index, acc, original ->
                        acc.replace(original, "generatedDefinitionId${index + 1}")
                    }

                println(scrubbedBody)

                approver.assertApproved(httpMessage.body(scrubbedBody))
            }

            override fun withNameSuffix(suffix: String): Approver {
                TODO("Not yet implemented")
            }
        }

        super.`renders as expected`(scrubbingApprover)
    }

    @Test
    fun `renders json schema bodies that use data classes`(approver: Approver) {
        val topLevelObjectLens = JTopLevelObject.autoBody().toLens()
        val example = TopLevelObject(NestedObject(null, 123))

        val router = "/basepath" newBind newContract {
            renderer = rendererToUse

            routes += "/body_json_schema_data_class" meta {
                receiving(topLevelObjectLens to example)
            } bindContract Method.POST to { _ -> Response(Status.OK) }

            routes += "/body_json_schema_data_class_with_definition_id" meta {
                receiving(topLevelObjectLens to example, "OverriddenDefinitionId", "a_prefix_")
            } bindContract Method.POST to { _ -> Response(Status.OK) }
        }

        approver.assertApproved(router(Request(Method.GET, "/basepath?the_api_key=somevalue")))
    }

    // this serves as an example of how the KondorJsonToJsonSchema can be used to render the OpenApi3 contract
    private class KondorOpenApi3Renderer(
        kondorJson: KondorJson,
        private val delegate: OpenApi3ApiRenderer<JsonNode>,
        refLocationPrefix: String = "components/schemas",
    ) : ApiRenderer<Api<JsonNode>, JsonNode> by delegate {
        private val jsonToJsonSchema = KondorJsonSchemaCreator(kondorJson, refLocationPrefix)

        override fun toSchema(
            obj: Any,
            overrideDefinitionId: String?,
            refModelNamePrefix: String?,
        ): JsonSchema<JsonNode> {
            return jsonToJsonSchema.toSchema(obj, overrideDefinitionId, refModelNamePrefix)
        }
    }
}
