package org.http4k.format.dataframe

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_CSV
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.JsonPath
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.io.ColType
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic.ARRAY_AND_VALUE_COLUMNS
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readJson
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Represents the extraction format for taking data and converting it into a DataFrame.
 */
sealed interface DataFrameFormat : (InputStream) -> AnyFrame {
    val contentType: ContentType
}

data class CSV(
    val delimiter: Char = ',',
    val header: List<String> = listOf(),
    val colTypes: Map<String, ColType> = mapOf(),
    val skipLines: Int = 0,
    val readLines: Int? = null,
    val duplicate: Boolean = true,
    val charset: Charset = Charsets.UTF_8,
    val parserOptions: ParserOptions? = null,
    override val contentType: ContentType = TEXT_CSV
) : DataFrameFormat {
    override operator fun invoke(input: InputStream) =
        DataFrame.readCSV(
            input,
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
}

data class JSON(
    val header: List<String> = emptyList(),
    val keyValuePaths: List<JsonPath> = emptyList(),
    val typeClashTactic: TypeClashTactic = ARRAY_AND_VALUE_COLUMNS,
    override val contentType: ContentType = APPLICATION_JSON
) : DataFrameFormat {
    override operator fun invoke(input: InputStream) =
        DataFrame.readJson(input, header, keyValuePaths, typeClashTactic)
}
