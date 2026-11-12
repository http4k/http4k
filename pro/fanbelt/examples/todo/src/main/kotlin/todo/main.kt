package todo

import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.lens.string
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.sqlite.SqliteDatabase
import org.http4k.fanbelt.store.sqlite.SqliteDocumentStorage
import org.http4k.fanbelt.store.sqlite.SqliteDocuments
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.Jetty
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import todo.domain.TodoList
import todo.store.Lists
import todo.store.TodoJson
import todo.store.TodoTables
import java.io.File

val SQLITE_FILE = EnvironmentKey.string().defaulted("SQLITE_FILE", "todos.db")

fun main() {
    val runtime = AppRuntime()
    val storage = SqliteDocumentStorage(localDatabase(File(SQLITE_FILE(ENV))), TodoJson)
    Lists(storage).create(TenantId.of("demo"), TodoList(runtime.clock.instant()))
    val server = App(runtime, { AppInfra(it, storage) }).asServer(Jetty(0)).start()
    Runtime.getRuntime().addShutdownHook(Thread(server::stop))
    println("Fanbelt todos on http://localhost:${server.port()}")
}

private fun localDatabase(file: File) = file.apply {
    transaction(SqliteDatabase(this)) {
        SchemaUtils.create(*listOf(TodoTables.TODOS, TodoTables.LISTS).map(SqliteDocuments::schema).toTypedArray())
    }
}