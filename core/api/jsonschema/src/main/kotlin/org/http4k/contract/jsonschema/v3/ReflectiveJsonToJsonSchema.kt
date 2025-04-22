package org.http4k.contract.jsonschema.v3

import org.http4k.contract.jsonschema.EmptyArray
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.JsonSchemaCreator
import org.http4k.contract.jsonschema.OneOfArray
import org.http4k.contract.jsonschema.SchemaNode
import org.http4k.contract.jsonschema.v3.SchemaModelNamer.Companion.Simple
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.*
import org.http4k.unquoted
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

class ReflectiveJsonToJsonSchema<NODE : Any>(
    private val json: AutoMarshallingJson<NODE>,
    private val fieldRetrieval: FieldRetrieval = FieldRetrieval.compose(
        SimpleLookup(metadataRetrievalStrategy = PrimitivesFieldMetadataRetrievalStrategy)
    ),
    private val modelNamer: SchemaModelNamer = Simple,
    private val refLocationPrefix: String = "components/schemas",
    private val metadataRetrieval: MetadataRetrieval = MetadataRetrieval.compose(SimpleMetadataLookup(emptyMap()))
) : JsonSchemaCreator<KClass<*>, NODE> {

    private fun KClass<*>.toSchemaNode(name: String?): SchemaNode = when {
        starProjectedType.isSubtypeOf(Iterable::class.starProjectedType) -> toArraySchemaNode(name)
        starProjectedType.isSubtypeOf(Enum::class.starProjectedType) -> toEnumSchemaNode(name)
        else -> toObjectOrMapSchemaNode(name)
    }

    private fun <T, V> KProperty1<T, V>.asIterableSchemaNode(): String {
        returnType
        TODO("Not yet implemented")
    }

    private fun KClass<*>.toArraySchemaNode(name: String?): SchemaNode {
        TODO("Not yet implemented")
    }

    private fun KProperty1<*, *>.toEnumSchemaNode(overriddenName: String?) = SchemaNode.Enum(
        overriddenName ?: modelNamer(name),
        StringParam,
        returnType.isMarkedNullable,
        (returnType.classifier as KClass<*>).java.enumConstants!![0],
        (returnType.classifier as KClass<*>).java.enumConstants!!.map { json.asFormatString(it).unquoted() },
        null
    )

    private fun KProperty1<*, *>.asIterableSchemaNode(): SchemaNode {
        SchemaNode.Array(name, returnType.isMarkedNullable, EmptyArray, null, null
    }

    private fun KProperty1<*, *>.asEnumSchemaNode() = SchemaNode.Enum(
        name, StringParam, returnType.isMarkedNullable, null,
        (returnType.classifier as KClass<Enum<*>>).java.enumConstants.map { it.name }, null
    )

    private fun KProperty1<*, *>.asObjectOrMapSchemaNode(): SchemaNode {
        TODO()
    }

    private fun KType.toParamMeta() = when (val classifier = classifier) {
        is KClass<*> -> when {
            classifier.isSubclassOf(Int::class) -> IntegerParam
            classifier.isSubclassOf(Boolean::class) -> BooleanParam
            classifier.isSubclassOf(Long::class) -> IntegerParam
            classifier.isSubclassOf(String::class) -> StringParam
            classifier.isSubclassOf(Number::class) -> NumberParam
            classifier.isSubclassOf(Iterable::class) -> ArrayParam(StringParam) // TODO fix
            else -> ObjectParam
        }

        else -> ObjectParam
    }

    private fun KClass<*>.toObjectOrMapSchemaNode(overriddenName: String?) = SchemaNode.Object(
        overriddenName ?: modelNamer(this),
        false,
        memberProperties.associate { it.name to it.toSchema() },
        null,
        null
    )

    private fun KProperty1<out Any, *>.toSchema(): SchemaNode = when {
        returnType.isSubtypeOf(Iterable::class.starProjectedType) -> asIterableSchemaNode()
        returnType.isSubtypeOf(Enum::class.starProjectedType) -> asEnumSchemaNode()
        else -> asObjectOrMapSchemaNode()
    }

    fun toSchemaNode(
        obj: KClass<*>,
        overrideDefinitionId: String? = null,
        refModelNamePrefix: String? = null
    ): JsonSchema<NODE> {
        val schemaNode = obj.toSchemaNode(overrideDefinitionId)
//        }


        val schema = JsonSchema(json.asJsonObject(schemaNode),
            schemaNode.definitions.map { it.name() to it }.toSet())


        val schema =
            json.asJsonObject(obj)
                .toSchemaNode(obj, overrideDefinitionId, true, refModelNamePrefix.orEmpty(), metadataRetrieval(obj))
        return JsonSchema(
            json.asJsonObject(schema),
            schema.definitions.map { it.name() to json.asJsonObject(it) }.distinctBy { it.first }.toSet()
        )
    }

    private fun NODE.toSchemaNode(
        value: Any,
        objName: String?,
        topLevel: Boolean,
        refModelNamePrefix: String,
        metadata: FieldMetadata?
    ) =
        when (val param = json.typeOf(this).toParam()) {
            is ArrayParam -> toArraySchema("", value, false, null, refModelNamePrefix)
            ObjectParam -> toObjectOrMapSchemaNode(
                objName,
                value,
                false,
                topLevel,
                metadata,
                refModelNamePrefix
            )

            else -> value.javaClass.enumConstants?.let {
                toEnumSchemaNode("", it[0], json.typeOf(this).toParam(), it, false, null, refModelNamePrefix)
            } ?: toSchemaNode("", param, false, metadata)
        }

    private fun NODE.toSchemaNode(name: String, paramMeta: ParamMeta, isNullable: Boolean, metadata: FieldMetadata?) =
        SchemaNode.Primitive(name, paramMeta, isNullable, this, metadata)

    private fun NODE.toArraySchema(
        name: String,
        obj: Any,
        isNullable: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode {
        val items = json.elements(this)
            .zip(items(obj)) { node: NODE, value: Any ->
                value.javaClass.enumConstants?.let {
                    node.toEnumSchemaNode("", it[0], json.typeOf(node).toParam(), it, false, null, refModelNamePrefix)
                } ?: node.toSchemaNode(
                    value,
                    null,
                    false,
                    refModelNamePrefix,
                    fieldRetrieval(FieldHolder(value), "value").metadata
                )
            }.map { it.arrayItem() }.toSet()

        val arrayItems = when (items.size) {
            0 -> EmptyArray
            1 -> items.first()
            else -> OneOfArray(items)
        }

        return SchemaNode.Array(name, isNullable, arrayItems, this, metadata)
    }

    private fun NODE.toEnumSchemaNode(
        fieldName: String, obj: Any, param: ParamMeta,
        enumConstants: Array<Any>, isNullable: Boolean, metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode =
        SchemaNode.Reference(
            fieldName,
            "#/$refLocationPrefix/$refModelNamePrefix${modelNamer(obj)}",
            SchemaNode.Enum(
                "$refModelNamePrefix${modelNamer(obj)}",
                param,
                isNullable,
                this,
                enumConstants.map { json.asFormatString(it).unquoted() },
                null
            ),
            metadata
        )

    private fun NODE.toObjectOrMapSchemaNode(
        objName: String?,
        obj: Any,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ) =
        if (obj is Map<*, *>) toMapSchema(objName, obj, isNullable, topLevel, metadata, refModelNamePrefix)
        else toObjectSchema(objName, obj, isNullable, topLevel, metadata, refModelNamePrefix)

    private fun NODE.toObjectSchema(
        objName: String?,
        obj: Any,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode {
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, fieldRetrieval(obj, it.first)) }
            .map { (fieldName, field, kField) ->
                makePropertySchemaFor(
                    field,
                    fieldName,
                    kField.value,
                    kField.isNullable,
                    kField.metadata,
                    refModelNamePrefix
                )
            }.associateBy { it.name() }

        val nameToUseForRef = if (topLevel) objName ?: modelNamer(obj) else modelNamer(obj)

        return SchemaNode.Reference(
            objName
                ?: modelNamer(obj), "#/$refLocationPrefix/$refModelNamePrefix$nameToUseForRef",
            SchemaNode.Object(refModelNamePrefix + nameToUseForRef, isNullable, properties, this, metadata), null
        )
    }

    private fun NODE.toMapSchema(
        objName: String?,
        obj: Map<*, *>,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode {
        val objWithStringKeys = obj.mapKeys { it.key?.let(::toJsonKey) }
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, objWithStringKeys[it.first]!!) }
            .map { (fieldName, field, value) ->
                makePropertySchemaFor(
                    field,
                    fieldName,
                    value,
                    true,
                    fieldRetrieval(FieldHolder(value), "value").metadata,
                    refModelNamePrefix
                )
            }.associateBy { it.name() }

        return if (topLevel && objName != null) {
            SchemaNode.Reference(
                objName, "#/$refLocationPrefix/$refModelNamePrefix$objName",
                SchemaNode.Object(refModelNamePrefix + objName, isNullable, properties, this, null), metadata
            )
        } else
            SchemaNode.MapType(
                objName ?: modelNamer(obj), isNullable,
                SchemaNode.Object(modelNamer(obj), isNullable, properties, this, null), metadata
            )
    }

    private fun makePropertySchemaFor(
        field: NODE,
        fieldName: String,
        value: Any,
        isNullable: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ) = when (val param = json.typeOf(field).toParam()) {
        is ArrayParam -> field.toArraySchema(fieldName, value, isNullable, metadata, refModelNamePrefix)
        ObjectParam -> field.toObjectOrMapSchemaNode(
            fieldName,
            value,
            isNullable,
            false,
            metadata,
            refModelNamePrefix
        )

        else -> with(field) {
            value.javaClass.enumConstants
                ?.let { toEnumSchemaNode(fieldName, value, param, it, isNullable, metadata, refModelNamePrefix) }
                ?: toSchemaNode(fieldName, param, isNullable, metadata)
        }
    }

    private fun toJsonKey(it: Any): String {
        return json.textValueOf(json.asJsonObject(MapKey(it)), "keyAsString")!!
    }
}
