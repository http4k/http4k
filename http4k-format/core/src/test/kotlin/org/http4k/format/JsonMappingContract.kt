package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.ParamMeta
import org.http4k.lens.enum
import org.http4k.lens.int
import org.junit.jupiter.api.Test

abstract class JsonMappingContract<NODE : Any>(private val json: Json<NODE>) {

    @Test
    fun `mapping json to object`() {
        val lens = json.body().map(toEmployee()).toLens()

        val employee: Employee = lens(Response(Status.OK).body(JSON))

        assertThat(employee, equalTo(Employee(
            name = "Betty",
            age = 33,
            department = Department.Technology,
            manager = Employee(
                name = "Susan",
                age = 37,
                department = Department.Technology
            ),
            skills = listOf("Coding", "Coffee-making", "http4k")
        )))
    }

    @Test
    fun `mapping json to array of objects`() {
        val lens = json.body().map(toEmployees()).toLens()

        val employees: List<Employee> = lens(Response(Status.OK).body("""[ $JSON ]"""))

        assertThat(employees, equalTo(listOf(Employee(
            name = "Betty",
            age = 33,
            department = Department.Technology,
            manager = Employee(
                name = "Susan",
                age = 37,
                department = Department.Technology
            ),
            skills = listOf("Coding", "Coffee-making", "http4k")
        ))))
    }

    private fun toEmployee(): (NODE) -> Employee = json {
        asA {
            Employee(
                name = field().required("name")[it],
                age = field().int().required("age")[it],
                department = field().enum<NODE, Department>().required("department")[it],
                manager = obj(toEmployee()).optional("manager")[it],
                skills = array(ParamMeta.StringParam, this::text).defaulted("skills", emptyList())[it]
            )
        }
    }

    private fun toEmployees(): (NODE) -> List<Employee> = json.asArray(toEmployee())

    companion object {
        private const val JSON = """
            {
                "name" : "Betty",
                "age" : 33,
                "department" : "Technology",
                "manager" : {
                    "name" : "Susan",
                    "age" : 37,
                    "department" : "Technology"
                },
                "skills" : [ "Coding", "Coffee-making", "http4k" ]
            }
            """
    }
}

data class Employee(
    val name: String,
    val age: Int,
    val department: Department,
    val manager: Employee? = null,
    val skills: List<String> = emptyList()
)

enum class Department {
    Technology
}
