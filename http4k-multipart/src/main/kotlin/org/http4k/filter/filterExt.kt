package org.http4k.filter

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.MultipartEntity
import org.http4k.core.body.form
import org.http4k.core.multipartIterator

/**
 * Process files on upload using the passed consumer, which returns a reference.
 * The form file is replaced in the form with this reference.
 */
fun ServerFilters.ProcessFiles(fileConsumer: (MultipartEntity.File) -> String) = Filter { next ->
    HttpHandler {
        val withProcessedFiles = it.multipartIterator().asSequence().fold(
            it.body(Body.EMPTY)
                .replaceHeader("content-type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
        ) { memo, next ->
            when (next) {
                is MultipartEntity.File -> {
                    memo.form(next.name, fileConsumer(next))
                }
                is MultipartEntity.Field -> memo.form(next.name, next.value)
            }
        }

        next(withProcessedFiles)
    }
}
