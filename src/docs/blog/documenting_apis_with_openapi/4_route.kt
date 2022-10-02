package blog.documenting_apis_with_openapi

import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path

data class Person(val name: String, val age: Age, val children: List<Person> = emptyList())

fun Family(): ContractRoute {

    val familyData = Person("Bob", Age(85), listOf(
        Person("Anita", Age(55)),
        Person("Donald", Age(52), listOf(Person("Don Jr", Age(21))))
    ))

    val responseLens = Body.auto<Person>("The matched family tree").toLens()

    fun handler(queryName: String): HttpHandler = {
        fun Person.search(): Person? = when (name) {
            queryName -> this
            else -> children.firstOrNull { it.search() != null }
        }

        familyData.search()?.let { Response(OK).with(responseLens of it) } ?: Response(NOT_FOUND)
    }

    return "/search" / Path.of("name", "The name to search for in the tree") meta {
        summary = "Search family tree"
        description = "Given a name, returns a sub family tree starting with that person"
        tags += Tag("query")
        returning(OK, responseLens to Person("Donald", Age(52), listOf(Person("Don Jr", Age(21)))), "Cut down family tree")
        returning(NOT_FOUND to "That person does not exist the family")
    } bindContract GET to ::handler
}
