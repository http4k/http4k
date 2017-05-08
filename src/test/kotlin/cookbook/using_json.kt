package cookbook

import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.with
import org.http4k.http.formats.Jackson
import org.http4k.http.formats.Jackson.asJsonArray
import org.http4k.http.formats.Jackson.asJsonObject
import org.http4k.http.formats.Jackson.asJsonValue
import org.http4k.http.formats.Jackson.asPrettyJsonString
import org.http4k.http.formats.Jackson.json
import org.http4k.http.lens.Body

fun main(args: Array<String>) {

    val json = Jackson

    val objectUsingExtensionFunctions =
        listOf(
            "thisIsAString" to "stringValue".asJsonValue(),
            "thisIsANumber" to 12345.asJsonValue(),
            "thisIsAList" to listOf(true.asJsonValue()).asJsonArray()
        ).asJsonObject()

    val objectUsingDirectApi = json.obj(
        "thisIsAString" to json.string("stringValue"),
        "thisIsANumber" to json.number(12345),
        "thisIsAList" to json.array(listOf(json.boolean(true)))
    )

    println(objectUsingExtensionFunctions.asPrettyJsonString())

    println(
        Response(OK).with(
            Body.json().required() to
                listOf(
                    objectUsingDirectApi,
                    objectUsingExtensionFunctions
                ).asJsonArray())
    )
}
