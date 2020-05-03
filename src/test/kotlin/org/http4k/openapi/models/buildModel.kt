package org.http4k.openapi.models

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.SchemaSpec

fun SchemaSpec.buildModelClass(name: String, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec {
    return when (this) {
        is SchemaSpec.RefSpec -> generated[name]
            ?: allSchemas[schemaName]!!.buildModelClass(schemaName, allSchemas, generated)
        is SchemaSpec.ObjectSpec -> {
            val typeSpec = buildModelClass(name, allSchemas, generated)
            generated[name] = typeSpec
            typeSpec
        }
        is SchemaSpec.ArraySpec -> buildModelClass(name, allSchemas, generated)
        else -> TypeSpec.classBuilder(name.capitalize()).build()
    }
}

private fun SchemaSpec.ObjectSpec.buildModelClass(name: String, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec {
    val props = properties.map { (name, spec) ->
        name to spec.buildModelClass(name, allSchemas, generated)
    }

    val primaryConstructor = FunSpec.constructorBuilder()
    props.forEach {
        primaryConstructor.addParameter(it.first, String::class)
    }

    val addModifiers = TypeSpec.classBuilder(name.capitalize())
        .addModifiers(DATA)
        .primaryConstructor(primaryConstructor.build())

    props.forEach {
        addModifiers.addProperty(PropertySpec.builder(it.first, String::class)
            .initializer(it.first)
            .build())
    }

    return addModifiers.build()
}

private fun SchemaSpec.ArraySpec.buildModelClass(name: String, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec {
    return TypeSpec.classBuilder(name.capitalize())
        .addModifiers(DATA)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .build()
}
