package cookbook.html_forms

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.body.form

fun main(args: Array<String>) {

    val request = Request(Method.GET, "/").form("name", "rita").form("age", "55")

    println(request.form("name"))
    println(request.form("age"))
}