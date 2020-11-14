package org.http4k.graphql.schema.models

import com.expediagroup.graphql.annotations.GraphQLDescription
import org.dataloader.DataLoader
import java.util.concurrent.CompletableFuture.supplyAsync

const val BATCH_BOOK_LOADER_NAME = "BATCH_BOOK_LOADER"

val batchBookLoader = DataLoader<List<Long>, List<Book>> { ids ->
    supplyAsync {
        val allBooks = Book.search(ids.flatten())
        ids.fold(mutableListOf()) { acc: MutableList<List<Book>>, idSet ->
            val matchingBooks = allBooks.filter { idSet.contains(it.id) }
            acc.add(matchingBooks)
            acc
        }
    }
}

@GraphQLDescription("Contains Book Metadata, title, authorship, and references to product and content.")
data class Book(
    val id: Long,
    val title: String
) {
    @Suppress("unused")
    companion object {
        fun search(ids: List<Long>): List<Book> {
            return listOf(
                Book(id = 1, title = "Campbell Biology"),
                Book(id = 2, title = "The Cell"),
                Book(id = 3, title = "Data Structures in C++"),
                Book(id = 4, title = "The Algorithm Design Manual")
            ).filter { ids.contains(it.id) }
        }
    }
}
