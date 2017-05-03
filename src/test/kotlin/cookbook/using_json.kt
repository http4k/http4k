package cookbook

import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.with
import org.reekwest.http.formats.Jackson
import org.reekwest.http.formats.Jackson.asJsonArray
import org.reekwest.http.formats.Jackson.asJsonObject
import org.reekwest.http.formats.Jackson.asJsonValue
import org.reekwest.http.formats.Jackson.asPrettyJsonString
import org.reekwest.http.formats.Jackson.json
import org.reekwest.http.lens.Body

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
