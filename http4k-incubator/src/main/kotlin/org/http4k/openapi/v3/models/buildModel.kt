package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.http4k.openapi.v3.SchemaSpec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addParameter
import org.http4k.poet.Property.Companion.addProperty

fun SchemaSpec.buildModelClass(className: ClassName, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec =
    when (this) {
        is SchemaSpec.ObjectSpec -> generated.getOrPut(className.simpleName, { buildModelClass(className, allSchemas, generated) })
        is SchemaSpec.RefSpec -> generated.getOrPut(schemaName, { allSchemas.getValue(schemaName).buildModelClass(ClassName(className.packageName, schemaName), allSchemas, generated) })
        is SchemaSpec.ArraySpec -> itemsSpec().buildModelClass(className, allSchemas, generated)
        else -> TypeSpec.classBuilder(className).build()
    }

private fun SchemaSpec.ObjectSpec.buildModelClass(className: ClassName, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec {
    val clazz = TypeSpec.classBuilder(className)
    val primaryConstructor = FunSpec.constructorBuilder()

    fun SchemaSpec.propertyType(): TypeName = when (this) {
        is SchemaSpec.ObjectSpec -> Map::class.parameterizedBy(String::class, Any::class)
        is SchemaSpec.ArraySpec -> List::class.asClassName().parameterizedBy(listOf(itemsSpec().propertyType()))
        is SchemaSpec.RefSpec -> {
            val refClassName = ClassName(className.packageName, schemaName)
            buildModelClass(refClassName, allSchemas, generated)
            refClassName
        }
        else -> this.clazz!!.asTypeName()
    }

    properties
        .map { (name, spec) -> Property(name, spec.propertyType().copy(nullable = !required.contains(name))) }
        .forEach {
            primaryConstructor.addParameter(it)
            clazz.addProperty(it)
        }

    if(additionalProperties != null || properties.isEmpty()) {
        val freeform = Property("additional", Map::class.parameterizedBy(String::class, Any::class))
        primaryConstructor.addParameter(freeform)
        clazz.addProperty(freeform)
    }

    clazz.addModifiers(DATA).primaryConstructor(primaryConstructor.build())

    return clazz.build()
}
