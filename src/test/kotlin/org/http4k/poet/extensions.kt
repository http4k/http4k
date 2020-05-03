package org.http4k.poet

import com.squareup.kotlinpoet.asTypeName
import org.http4k.openapi.ParameterSpec

fun ParameterSpec.asTypeName() = schema.clazz?.asTypeName()?.copy(nullable = !required)
