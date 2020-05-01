package org.http4k.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class Property(val name: String, val type: TypeName, vararg val modifiers: KModifier) {
    constructor(name: String, vararg modifiers: KModifier) : this(ClassName.bestGuess(name).simpleName, ClassName.bestGuess(name), *modifiers)

    companion object {
        inline operator fun <reified T> invoke(vararg modifiers: KModifier) =
            with(T::class) { Property(simpleName!!, ClassName.bestGuess(qualifiedName!!), *modifiers) }

        fun FunSpec.Builder.addParameter(property: Property) = addParameter(ParameterSpec.builder(property.name, property.type, *property.modifiers).build())
        fun TypeSpec.Builder.addProperty(property: Property) = addProperty(PropertySpec.builder(property.name, property.type, KModifier.PRIVATE).initializer(property.name).build())
    }
}
