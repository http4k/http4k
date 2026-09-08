package org.http4k.format

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import java.io.StringWriter
import kotlin.reflect.KClass

open class ConfigurableJacksonCsv(val mapper: CsvMapper, val defaultContentType: ContentType = ContentType.TEXT_CSV) {

    inline fun <reified T> defaultWriteSchema(): CsvSchema = mapper.schemaFor(T::class.java).withHeader()
    fun defaultReadSchema(): CsvSchema = CsvSchema.emptySchema().withHeader()

    fun <T : Any> writerFor(type: KClass<T>, schema: CsvSchema): (List<T>) -> String = { body: List<T> ->
        StringWriter().use { stringWriter ->
            mapper.writerFor(type.java).with(schema).writeValues(stringWriter).use {
                it.writeAll(body)
            }
            stringWriter
        }.toString()
    }

    fun <T : Any> readerFor(type: KClass<T>, schema: CsvSchema): (String) -> List<T> {
        val reader = mapper.readerFor(type.java).with(schema)
        return { body: String ->
            reader.readValues<T>(body).readAll()
        }
    }

    inline fun <reified T : Any> writeCsv(input: List<T>, schema: CsvSchema = defaultWriteSchema<T>()): String {
        val writer = writerFor(T::class, schema)
        return writer(input)
    }

    inline fun <reified T : Any> readCsv(input: String, schema: CsvSchema = defaultReadSchema()): List<T> {
        val reader = readerFor(T::class, schema)
        return reader(input)
    }

    /**
     * Convenience function to write the object as CSV to the message body and set the content type.
     */
    inline fun <reified T : Any, R : HttpMessage> R.csv(t: List<T>): R = with(Body.auto<T>().toLens() of t)

    /**
     * Convenience function to read an object as CSV from the message body.
     */
    inline fun <reified T : Any> HttpMessage.csv(): List<T> = Body.auto<T>().toLens()(this)

    inline fun <reified T : Any> asBiDiMapping(
        readSchema: CsvSchema = defaultReadSchema(),
        writeSchema: CsvSchema = defaultWriteSchema<T>()
    ) =
        BiDiMapping<String, List<T>>(readerFor(T::class, readSchema), writerFor(T::class, writeSchema))

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None
    ) = autoBody<T>(description, contentNegotiation)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType,
        readSchema: CsvSchema = defaultReadSchema(),
        writeSchema: CsvSchema = defaultWriteSchema<T>()
    ): BiDiBodyLensSpec<List<T>> {
        val reader = readerFor(T::class, readSchema)
        val writer = writerFor(T::class, writeSchema)

        return httpBodyRoot(
            listOf(Meta(true, "body", ObjectParam, "body", description, emptyMap())),
            contentType,
            contentNegotiation
        )
            .map({ it.payload.asString() }, { Body(it) })
            .map(reader, writer)
    }
}
