package cookbook.graphql

import com.expediagroup.graphql.annotations.GraphQLDescription

/**
 * Provide Search options for book data
 */
class BookQueryService {
    @GraphQLDescription("Return list of books based on BookSearchParameter options")
    @Suppress("unused")
    fun searchBooks(params: BookQuery) = Book.search(params.ids)
}

data class BookQuery(val ids: List<Long>)
