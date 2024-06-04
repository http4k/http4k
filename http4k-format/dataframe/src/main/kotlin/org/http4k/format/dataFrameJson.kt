package org.http4k.format

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.binary
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.JsonPath
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic.ARRAY_AND_VALUE_COLUMNS
import org.jetbrains.kotlinx.dataframe.io.readJson

/**
 * Create a BodyLens for DataFrame from JSON
 */
fun <T> Body.Companion.dataFrameJson(
    options: DataFrameJsonOptions = DataFrameJsonOptions(),
    contentType: ContentType = APPLICATION_JSON,
    description: String? = null,
    contentNegotiation: ContentNegotiation = None
) = binary(contentType, description, contentNegotiation).map {
    with(options) { DataFrame.readJson(it, header, keyValuePaths, typeClashTactic).cast<T>() }
}

/**
 * Read a DataFrame from the body of the message
 */
fun <T> HttpMessage.dataFrameJson(options: DataFrameJsonOptions = DataFrameJsonOptions()) =
    Body.dataFrameJson<T>(options).toLens()(this)

/**
 * Options for reading DataFrame from JSON
 */
data class DataFrameJsonOptions(
    val header: List<String> = emptyList(),
    val keyValuePaths: List<JsonPath> = emptyList(),
    val typeClashTactic: TypeClashTactic = ARRAY_AND_VALUE_COLUMNS,
)
