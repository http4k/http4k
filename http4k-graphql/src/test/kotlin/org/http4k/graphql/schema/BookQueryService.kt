package org.http4k.graphql.schema

import com.expediagroup.graphql.annotations.GraphQLDescription
import org.http4k.graphql.schema.models.Book

/**
 * Provide Search options for book data
 */
class BookQueryService {
    @GraphQLDescription("Return list of books based on BookSearchParameter options")
    @Suppress("unused")
    fun searchBooks(params: BookSearchParameters) = Book.search(params.ids)
}

data class BookSearchParameters(val ids: List<Long>)
