package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

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
}
