package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.readJson


@DataSchema(isOpen = false)
interface Service1 {
    val url: String
}

@DataSchema
interface Service {
    val _links: Service1
    val name: String
    public companion object {
        public const val defaultPath: kotlin.String = "src/test/resources/services.json"

        public val defaultKeyValuePaths: kotlin.collections.List<org.jetbrains.kotlinx.dataframe.api.JsonPath> = listOf()

        public val defaultTypeClashTactic: org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic = org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic.ARRAY_AND_VALUE_COLUMNS

        public val defaultUnifyNumbers: kotlin.Boolean = true

        public fun readJson(
            path: kotlin.String = defaultPath,
            keyValuePaths: kotlin.collections.List<org.jetbrains.kotlinx.dataframe.api.JsonPath> = defaultKeyValuePaths,
            typeClashTactic: org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic = defaultTypeClashTactic,
            unifyNumbers: kotlin.Boolean = defaultUnifyNumbers,
            verify: kotlin.Boolean? = null,
        ): org.jetbrains.kotlinx.dataframe.DataFrame<Service> {
            val df = DataFrame.readJson(path, keyValuePaths = keyValuePaths, typeClashTactic = typeClashTactic, unifyNumbers = unifyNumbers)
            return if (verify != null) df.cast(verify = verify) else df.cast()
        }
    }

}

@DataSchema
interface Repository {
    @ColumnName("full_name")
    val fullName: String
    @ColumnName("html_url")
    val htmlUrl: java.net.URL
    @ColumnName("stargazers_count")
    val stargazersCount: Int
    val topics: String
    val watchers: Int
    public companion object {
        public const val defaultPath: kotlin.String = "src/test/resources/repositories.csv"

        public val defaultDelimiter: kotlin.Char = ','

        public fun readCsv(
            path: kotlin.String = defaultPath,
            delimiter: kotlin.Char = defaultDelimiter,
            verify: kotlin.Boolean? = null,
        ): org.jetbrains.kotlinx.dataframe.DataFrame<Repository> {
            val df = DataFrame.readCsv(path, delimiter = delimiter)
            return if (verify != null) df.cast(verify = verify) else df.cast()
        }
    }

}
