package guide.modules.json

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.format.Moshi.auto

data class MyIntWrapper(val value: Int)

fun main() {
    val aListLens = Body.auto<List<MyIntWrapper>>().toLens()

    val req = Request(GET, "/").body(""" [ {"value":1}, {"value":2} ] """)

    val extractedList = aListLens(req)

    val nativeList = listOf(MyIntWrapper(1), MyIntWrapper(2))

    println(nativeList)
    println(extractedList)
    println(extractedList == nativeList)

    //solution:
    val anArrayLens = Body.auto<Array<MyIntWrapper>>().toLens()

    println(anArrayLens(req).contentEquals(arrayOf(MyIntWrapper(1), MyIntWrapper(2))))

// produces:
//    [MyIntWrapper(value=1), MyIntWrapper(value=2)]
//    [{value=1}, {value=2}]
//    false
//    true
}
