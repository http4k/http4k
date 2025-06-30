@file:Suppress("UNCHECKED_CAST")

package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.ColumnsScope
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataRow
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

@DataSchema(isOpen = false)
interface Service1 {
    val url: String
}

@DataSchema
interface Service {
    val _links: Service1
    val name: String
}

val DataRow<Repository>.fullName: String @JvmName("Repository_fullName") get() = this["full_name"] as String
val DataRow<Repository>.htmlUrl: java.net.URL @JvmName("Repository_htmlUrl") get() = this["html_url"] as java.net.URL
val DataRow<Repository>.stargazersCount: Int @JvmName("Repository_stargazersCount") get() = this["stargazers_count"] as Int

val DataRow<Service>._links: DataRow<Service1> @JvmName("Service__links") get() = this["_links"] as DataRow<Service1>
val ColumnsScope<Service>.name: DataColumn<String> @JvmName("Service_name") get() = this["name"] as DataColumn<String>
val DataRow<Service>.name: String @JvmName("Service_name") get() = this["name"] as String
val ColumnsScope<Service?>.name: DataColumn<String?> @JvmName("NullableService_name") get() = this["name"] as DataColumn<String?>
val DataRow<Service?>.name: String? @JvmName("NullableService_name") get() = this["name"] as String?

val ColumnsScope<Service1>.url: DataColumn<String> @JvmName("Service1_url") get() = this["url"] as DataColumn<String>
val DataRow<Service1>.url: String @JvmName("Service1_url") get() = this["url"] as String
val ColumnsScope<Service1?>.url: DataColumn<String?> @JvmName("NullableService1_url") get() = this["url"] as DataColumn<String?>
val DataRow<Service1?>.url: String? @JvmName("NullableService1_url") get() = this["url"] as String?
