package tutorials.typesafe_configuration.post

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.BiDiLens
import org.http4k.lens.Query
import org.http4k.lens.int

data class Page(val value: Int) {
    init {
        require(value > 1) { "Page number must be positive" }
    }
}

val lens: BiDiLens<Request, Page> = Query.int().map(::Page, Page::value).defaulted("pageNumber", Page(1))

val pageNumber: Page = lens(Request(GET, "http://abc/search?pageNumber=55"))

val updatedRequest: Request = lens(pageNumber, Request(GET, "http://abc/search"))
