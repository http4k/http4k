package org.http4k.format

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_CSV
import org.http4k.core.HttpMessage
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.binary
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.ColType
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8

/**
 * Create a BodyLens for DataFrame from CSV
 */
fun <T> Body.Companion.dataFrameCsv(
    options: DataFrameCSVOptions = DataFrameCSVOptions(),
    contentType: ContentType = TEXT_CSV,
    description: String? = null,
    contentNegotiation: ContentNegotiation = ContentNegotiation.None
) = binary(contentType, description, contentNegotiation)
    .map {
        with(options) {
            DataFrame.readCSV(
                it,
                delimiter,
                header,
                false,
                colTypes,
                skipLines,
                readLines,
                duplicate,
                charset,
                parserOptions
            )
        }.cast<T>()
    }

/**
 * Read a DataFrame from the body of the message
 */
fun <T> HttpMessage.dataFrameCsv(options: DataFrameCSVOptions = DataFrameCSVOptions()) =
    Body.dataFrameCsv<T>(options).toLens()(this)

/**
 * Options for reading DataFrame from CSV
 */
data class DataFrameCSVOptions(
    val delimiter: Char = ',',
    val header: List<String> = listOf(),
    val colTypes: Map<String, ColType> = mapOf(),
    val skipLines: Int = 0,
    val readLines: Int? = null,
    val duplicate: Boolean = true,
    val charset: Charset = UTF_8,
    val parserOptions: ParserOptions? = null,
)
