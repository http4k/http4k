package org.http4k.openapi.models

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.OpenApi3Spec

fun OpenApi3Spec.buildClass(name: String) = TypeSpec.classBuilder(name.capitalize())
    .addModifiers(KModifier.DATA)
    .primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("name", String::class)
        .build())
    .addProperty(PropertySpec.builder("name", String::class)
        .initializer("name")
        .build())
    .build()
