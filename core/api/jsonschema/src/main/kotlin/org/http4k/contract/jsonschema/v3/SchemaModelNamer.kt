package org.http4k.contract.jsonschema.v3

fun interface SchemaModelNamer : (Any) -> String {
    companion object {
        val Simple: SchemaModelNamer = SchemaModelNamer { it.javaClass.simpleName }
        val Full: SchemaModelNamer = SchemaModelNamer { it.javaClass.name }
        val Canonical: SchemaModelNamer = SchemaModelNamer { it.javaClass.canonicalName }
    }
}
