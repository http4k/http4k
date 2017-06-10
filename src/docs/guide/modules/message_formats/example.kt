package guide.modules.message_formats

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asJsonArray
import org.http4k.format.Jackson.asJsonObject
import org.http4k.format.Jackson.asJsonValue
import org.http4k.format.Jackson.asPrettyJsonString
import org.http4k.format.Jackson.json

// Extension method API:
val json = Jackson

val objectUsingExtensionFunctions: JsonNode =
    listOf(
        "thisIsAString" to "stringValue".asJsonValue(),
        "thisIsANumber" to 12345.asJsonValue(),
        "thisIsAList" to listOf(true.asJsonValue()).asJsonArray()
    ).asJsonObject()

val s = objectUsingExtensionFunctions.asPrettyJsonString()

// Direct JSON library API:
val objectUsingDirectApi: JsonNode = json.obj(
    "thisIsAString" to json.string("stringValue"),
    "thisIsANumber" to json.number(12345),
    "thisIsAList" to json.array(listOf(json.boolean(true)))
)

val response = Response(Status.OK).with(
    Body.json().toLens() of json.array(listOf(objectUsingDirectApi, objectUsingExtensionFunctions))
)