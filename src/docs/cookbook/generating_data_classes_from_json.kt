package cookbook

import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.GenerateDataClasses
import org.http4k.format.Jackson

/**
 * This example show the usage of the GenerateDataClasses to generate Kotlin data class
 * code for JSON messages from an endpoint
 */
fun main(args: Array<String>) {

    val request = Request(GET, "http://api.icndb.com/jokes/random/3")

    GenerateDataClasses(Jackson, System.out).then(ApacheClient()).invoke(request)
}
