package guide.modules.json

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asJsonArray
import org.http4k.format.Jackson.asJsonObject
import org.http4k.format.Jackson.asJsonValue
import org.http4k.format.Jackson.asPrettyJsonString
import org.http4k.format.Jackson.json
import org.http4k.format.Xml.xml
import org.w3c.dom.Node

val json = Jackson

// Extension method API:

val objectUsingExtensionFunctions: JsonNode =
    listOf(
        "thisIsAString" to "stringValue".asJsonValue(),
        "thisIsANumber" to 12345.asJsonValue(),
        "thisIsAList" to listOf(true.asJsonValue()).asJsonArray()
    ).asJsonObject()

val jsonString: String = objectUsingExtensionFunctions.asPrettyJsonString()

// Direct JSON library API:
val objectUsingDirectApi: JsonNode = json.obj(
    "thisIsAString" to json.string("stringValue"),
    "thisIsANumber" to json.number(12345),
    "thisIsAList" to json.array(listOf(json.boolean(true)))
)

// DSL JSON library API:
val objectUsingDslApi: JsonNode = json {
    obj(
        "thisIsAString" to string("stringValue"),
        "thisIsANumber" to number(12345),
        "thisIsAList" to array(listOf(boolean(true)))
    )
}

val response = Response(OK).with(
    Body.json().toLens() of json.array(listOf(objectUsingDirectApi, objectUsingExtensionFunctions, objectUsingDslApi))
)

val xmlLens = Body.xml().toLens()

val xmlNode: Node = xmlLens(Request(GET, "").body("<xml/>"))
