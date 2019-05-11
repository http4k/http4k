package org.http4k.format

import argo.jdom.JsonNode

class ArgoTest : JsonContract<JsonNode>(Argo) {
    override val prettyString = """{
	"hello": "world"
}"""
}

class ArgoGenerateDataClassesTest : GenerateDataClassesContract<JsonNode>(Argo)
