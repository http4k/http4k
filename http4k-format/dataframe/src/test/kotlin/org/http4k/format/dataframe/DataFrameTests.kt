package org.http4k.format.dataframe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.jetbrains.kotlinx.dataframe.impl.DataFrameSize
import org.jetbrains.kotlinx.dataframe.size
import org.junit.jupiter.api.Test

class DataFrameTests {

    @Test
    fun `can extract a set of untyped records from an HTTP message`() {
        val request = Request(GET, "").body(javaClass.getResourceAsStream("/repositories.csv")!!)
        val repos = request.dataFrame(CSV())

        assertThat(repos.size(), equalTo(DataFrameSize(5, 4)))
        assertThat(repos[0]["full_name"], equalTo("JPS"))
        assertThat(repos[0]["html_url"].toString(), equalTo("https://github.com/JPS"))
        assertThat(repos[0]["stargazers_count"].toString(), equalTo("23"))
    }
}
