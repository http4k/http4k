package org.http4k.openapi.models

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.http4k.openapi.SchemaSpec
import org.http4k.poet.Property
import org.http4k.poet.Property.Companion.addParameter
import org.http4k.poet.Property.Companion.addProperty

fun SchemaSpec.buildModelClass(name: String, allSchemas: Map<String, SchemaSpec>, generated: MutableMap<String, TypeSpec>): TypeSpec {
    return when (this) {
        is SchemaSpec.RefSpec -> generated[name]
            ?: allSchemas.getValue(schemaName).buildModelClass(schemaName, allSchemas, generated)
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
    val clazz = TypeSpec.classBuilder(name.capitalize())
    val primaryConstructor = FunSpec.constructorBuilder()

    val props = properties.map { (name, spec) ->
        val type = when (spec) {
            is SchemaSpec.ObjectSpec -> Map::class.parameterizedBy(String::class, Any::class)
            is SchemaSpec.ArraySpec -> {
                val typeArguments = listOf(ClassName.bestGuess("Any"))
                List::class.asClassName().parameterizedBy(typeArguments)
            }
            is SchemaSpec.RefSpec -> {
                spec.buildModelClass(name, allSchemas, generated)
                ClassName.bestGuess(spec.schemaName)
            }
            else -> spec.clazz!!.asTypeName()
        }.copy(nullable = !required.contains(name))

        Property(name, type)
    }

    props.forEach {
        primaryConstructor.addParameter(it)
        clazz.addProperty(it)
    }

    clazz.addModifiers(DATA).primaryConstructor(primaryConstructor.build())

    return clazz.build()
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
