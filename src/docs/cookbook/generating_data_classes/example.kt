package cookbook.generating_data_classes

import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.GenerateDataClasses
import org.http4k.format.Jackson

fun main() {

    val request = Request(GET, "http://api.icndb.com/jokes/random/3")

    GenerateDataClasses(Jackson, System.out).then(ApacheClient()).invoke(request)
}
