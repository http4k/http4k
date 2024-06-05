package guide.reference.dataframe

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.dataframe.CSV
import org.http4k.format.dataframe.dataFrame
import org.http4k.format.dataframe.dataFrameCsv
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.jetbrains.kotlinx.dataframe.api.filter

fun main() {

    // define a simple CSV endpoint
    val app = { _: Request ->
        Response(OK).body(
            """
            full_name,html_url,stargazers_count
            http4k,https://http4k.org,100
            http4k-connect,https://http4k-connect.org,10
            forkhandles,https://forkhandles.dev,20
        """.trimIndent()
        )
    }

    app.asServer(SunHttp(8000)).start()

    val response = JavaHttpClient()(Request(GET, "http://localhost:8000"))

    // load all the data into an untyped DataFrame
    val frame = response.dataFrame(CSV())

    // filter all projects starting with "h"
    val all_h_projects = frame
        .filter { it["full_name"]?.toString()?.startsWith("h") ?: false }

    println(all_h_projects)

    // you can also use the Kotlin KSP DataFrame plugin to generate typed data classes.
    // Then use the `dataFrameCsv` extension function to cast the DataFrame to a typed DataFrame

    // this is a fake class - we would the generated one in a real project and to manipulate
    // the dataframe as above
    data class Repository(val fullName: String)

    println(response.dataFrameCsv<Repository>())
}
