package org.http4k.poet

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.asTypeName
import org.http4k.openapi.ParameterSpec

fun ParameterSpec.asTypeName() = schema.clazz?.asTypeName()?.copy(nullable = !required)

fun FileSpec.Builder.buildFormatted() = this.indent("\t").build()
