package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Object
import java.io.PrintStream
import java.util.*

class GenerateDataClasses<ROOT : NODE, out NODE : Any>(private val json: Json<ROOT, NODE>, private val out: PrintStream = System.out) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler =
        { req ->
            val response = next(req)
            val process = process("Base", json.body().required()(response))
            process.flatMap { it.toList() }.toSet().mapNotNull(Gen::asDefinitionString).map(out::println)
            response
        }

    interface Gen : Iterable<Gen> {
        override fun iterator(): Iterator<Gen> = listOf(this).iterator()
        fun asClassName(): String
        fun asDefinitionString(): String? = null
    }

    enum class Primitives(val clazz: String) : Gen {
        Number("Number"), StringValue("String"), Boolean("Boolean"), Null("Any");

        override fun asClassName() = clazz
    }

    data class ArrayGen(val elements: Set<Gen>) : Gen {
        override fun asClassName(): String = "List<${(elements.firstOrNull() ?: Primitives.Null).asClassName()}>"
    }

    data class ObjectGen(val clazz: String = "", val fields: Map<String, Gen> = emptyMap()) : Gen {
        override fun asClassName(): String = clazz.capitalize()

        override fun iterator(): Iterator<Gen> = fields.map { it.value }.plus(object : Gen {
            override fun asClassName(): String = clazz.capitalize()
            override fun asDefinitionString(): String = """data class ${clazz.capitalize()}(${fields.map { "val ${it.key}: ${it.value.asClassName()}" }.joinToString(", ")})"""
        }).iterator()
    }

    private fun process(name: String, node: NODE): Gen = when (json.typeOf(node)) {
        Object -> ObjectGen(name, json.fields(node).map { it.first to process(it.first, it.second) }.toMap())
        Array -> ArrayGen(json.elements(node).flatMap { process("Array" + Random().nextInt(), it) }.toSet())
        JsonType.String -> Primitives.StringValue
        JsonType.Number -> Primitives.Number
        JsonType.Boolean -> Primitives.Boolean
        JsonType.Null -> Primitives.Null
    }
}
