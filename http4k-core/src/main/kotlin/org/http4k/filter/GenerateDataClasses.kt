package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Object
import java.io.PrintStream
import kotlin.random.Random

/**
 * This Filter is used to generate Data class definitions from a Response containing JSON. The Filter will try and reduce
 * the number of class definitions by selecting the definition with the most fields (for cases where lists of items
 * have different fields).
 */
class GenerateDataClasses<out NODE>(
    private val json: Json<NODE>,
    private val out: PrintStream = System.out,
    private val idGenerator: () -> Int = { Math.abs(Random.nextInt()) }
) : Filter {

    private fun flatten(list: Set<Gen>): Set<Gen> = list.flatMap { it }.toSet().let { if (it == list) list else flatten(it) }

    override fun invoke(next: HttpHandler): HttpHandler = { req ->
        val response = next(req)
        out.println("// result generated from ${req.uri}\n")
        out.println(flatten(setOf(process("Base", json.body().toLens()(response))))
            .toSet()
            .groupBy { it.asClassName() }
            .mapNotNull { (_, gens) -> gens.mapNotNull(Gen::asDefinitionString).sortedByDescending { it.length }.firstOrNull() }
            .sorted()
            .joinToString("\n\n"))
        response
    }

    interface Gen : Iterable<Gen> {
        override fun iterator(): Iterator<Gen> = listOf(this).iterator()
        fun asClassName(): String
        fun asDefinitionString(): String? = null
    }

    enum class Primitives(private val clazz: String) : Gen {
        Number("Number"), StringValue("String"), Boolean("Boolean"), Null("Any");

        override fun asClassName() = clazz
    }

    data class ArrayGen(val elements: Set<Gen>) : Gen {
        override fun asClassName(): String =
            with(if (elements.size == 1) elements.first() else Primitives.Null) {
                "List<${asClassName()}>"
            }

        override fun iterator(): Iterator<Gen> = elements.iterator()
    }

    data class ObjectGen(private val clazz: String = "", val fields: Map<String, Gen> = emptyMap()) : Gen {
        override fun asClassName(): String = clazz.capitalize()
        override fun iterator(): Iterator<Gen> = fields.map { it.value }.plus(listOf(this)).toSet().iterator()
        override fun asDefinitionString(): String = """data class ${clazz.capitalize()}(${fields.map { "val ${it.key}: ${it.value.asClassName()}?" }.joinToString(", ")})"""
    }

    private fun process(name: String, node: NODE): Gen = json {
        when (typeOf(node)) {
            Object -> ObjectGen(name, fields(node).map { it.first to process(it.first, it.second) }.toMap())
            Array -> {
                val arrayName = name.capitalize() + idGenerator()
                ArrayGen(json.elements(node).map { process(arrayName, it) }.toSet())
            }
            JsonType.String -> Primitives.StringValue
            JsonType.Integer, JsonType.Number -> Primitives.Number
            JsonType.Boolean -> Primitives.Boolean
            JsonType.Null -> Primitives.Null
        }
    }
}
