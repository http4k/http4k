@file:Suppress("UNCHECKED_CAST", "USELESS_CAST")

package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.ColumnsScope
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataRow

val ColumnsScope<Repository>.fullName: DataColumn<String> @JvmName("Repository_fullName") get() = this["full_name"] as DataColumn<String>
val DataRow<Repository>.fullName: String @JvmName("Repository_fullName") get() = this["full_name"] as String
val ColumnsScope<Repository?>.fullName: DataColumn<String?> @JvmName("NullableRepository_fullName") get() = this["full_name"] as DataColumn<String?>
val DataRow<Repository?>.fullName: String? @JvmName("NullableRepository_fullName") get() = this["full_name"] as String?
val ColumnsScope<Repository>.htmlUrl: DataColumn<java.net.URL> @JvmName("Repository_htmlUrl") get() = this["html_url"] as DataColumn<java.net.URL>
val DataRow<Repository>.htmlUrl: java.net.URL @JvmName("Repository_htmlUrl") get() = this["html_url"] as java.net.URL
val ColumnsScope<Repository?>.htmlUrl: DataColumn<java.net.URL?> @JvmName("NullableRepository_htmlUrl") get() = this["html_url"] as DataColumn<java.net.URL?>
val DataRow<Repository?>.htmlUrl: java.net.URL? @JvmName("NullableRepository_htmlUrl") get() = this["html_url"] as java.net.URL?
val ColumnsScope<Repository>.stargazersCount: DataColumn<Int> @JvmName("Repository_stargazersCount") get() = this["stargazers_count"] as DataColumn<Int>
val DataRow<Repository>.stargazersCount: Int @JvmName("Repository_stargazersCount") get() = this["stargazers_count"] as Int
val ColumnsScope<Repository?>.stargazersCount: DataColumn<Int?> @JvmName("NullableRepository_stargazersCount") get() = this["stargazers_count"] as DataColumn<Int?>
val DataRow<Repository?>.stargazersCount: Int? @JvmName("NullableRepository_stargazersCount") get() = this["stargazers_count"] as Int?
val ColumnsScope<Repository>.topics: DataColumn<String> @JvmName("Repository_topics") get() = this["topics"] as DataColumn<String>
val DataRow<Repository>.topics: String @JvmName("Repository_topics") get() = this["topics"] as String
val ColumnsScope<Repository?>.topics: DataColumn<String?> @JvmName("NullableRepository_topics") get() = this["topics"] as DataColumn<String?>
val DataRow<Repository?>.topics: String? @JvmName("NullableRepository_topics") get() = this["topics"] as String?
val ColumnsScope<Repository>.watchers: DataColumn<Int> @JvmName("Repository_watchers") get() = this["watchers"] as DataColumn<Int>
val DataRow<Repository>.watchers: Int @JvmName("Repository_watchers") get() = this["watchers"] as Int
val ColumnsScope<Repository?>.watchers: DataColumn<Int?> @JvmName("NullableRepository_watchers") get() = this["watchers"] as DataColumn<Int?>
val DataRow<Repository?>.watchers: Int? @JvmName("NullableRepository_watchers") get() = this["watchers"] as Int?
