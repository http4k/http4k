package org.http4k.format

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.httpBodyRoot
import java.io.StringWriter
import kotlin.reflect.KClass

open class ConfigurableJacksonCsv(val mapper: CsvMapper, val defaultContentType: ContentType = ContentType.TEXT_CSV) {

    inline fun <reified T> defaultSchema(): CsvSchema = mapper.schemaFor(T::class.java).withHeader()

    fun <T: Any> writerFor(type: KClass<T>, schema: CsvSchema): (List<T>) -> String {
        val writer = mapper.writerFor(type.java).with(schema)
        return { body: List<T> ->
            StringWriter().use { stringWriter ->
                writer.writeValues(stringWriter)
                    .writeAll(body)
                stringWriter
            }.toString()
        }
    }

    fun <T: Any> readerFor(type: KClass<T>, schema: CsvSchema): (String) -> List<T> {
        val reader = mapper.readerFor(type.java).with(schema)
        return { body: String ->
            reader.readValues<T>(body).readAll()
        }
    }

    inline fun <reified T: Any> writeCsv(input: List<T>, schema: CsvSchema = defaultSchema<T>()): String {
        val writer = writerFor(T::class, schema)
        return writer(input)
    }

    inline fun <reified T: Any> readCsv(input: String , schema: CsvSchema = defaultSchema<T>()): List<T> {
        val reader = readerFor(T::class, schema)
        return reader(input)
    }

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None
    ) = autoBody<T>(description, contentNegotiation)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType,
        schema: CsvSchema = defaultSchema<T>()
    ): BiDiBodyLensSpec<List<T>> {
        val reader = readerFor(T::class, schema)
        val writer = writerFor(T::class, schema)

        return httpBodyRoot(
            listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)),
            contentType,
            contentNegotiation
        )
            .map({ it.payload.asString() }, { Body(it) })
            .map(reader, writer)
    }
}
