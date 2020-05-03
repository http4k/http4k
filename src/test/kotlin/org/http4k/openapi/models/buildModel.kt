package org.http4k.openapi.models

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.SchemaSpec

fun SchemaSpec.buildClass(name: String, generated: Map<String, TypeSpec>): TypeSpec {
    return TypeSpec.classBuilder(name.capitalize())
        .addModifiers(KModifier.DATA)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .build()
}
