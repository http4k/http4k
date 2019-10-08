package org.http4k.format

import kotlinx.serialization.json.JsonElement

class KotlinxSerializationTest : JsonContract<JsonElement>(KotlinxSerialization) {
    override val prettyString = """{
	"hello": "world"
}"""
}

class ArgoGenerateDataClassesTest : GenerateDataClassesContract<JsonElement>(KotlinxSerialization)
