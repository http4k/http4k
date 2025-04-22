package org.http4k.contract.jsonschema.v3

import kotlin.reflect.KClass

fun interface SchemaModelNamer : (Any) -> String {
    companion object {
        val Simple: SchemaModelNamer = SchemaModelNamer {
            if (it is KClass<*>) it.simpleName ?: it.java.simpleName else it.javaClass.simpleName
        }
        val Full: SchemaModelNamer = SchemaModelNamer {
            if (it is KClass<*>) it.qualifiedName ?: it.java.name else it.javaClass.name
        }
        val Canonical: SchemaModelNamer = SchemaModelNamer {
            if (it is KClass<*>) it.qualifiedName ?: it.java.canonicalName else it.javaClass.canonicalName
        }
    }
}
