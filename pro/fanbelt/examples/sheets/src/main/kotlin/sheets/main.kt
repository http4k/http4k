package sheets

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
import sheets.domain.SheetDetails
import sheets.store.SheetJson
import sheets.store.SheetTables
import sheets.store.Sheets
import java.io.File

val SQLITE_FILE = EnvironmentKey.string().defaulted("SQLITE_FILE", "sheets.db")

fun main() {
    val runtime = AppRuntime()
    val storage = SqliteDocumentStorage(localDatabase(File(SQLITE_FILE(ENV))), SheetJson)
    Sheets(storage).create(TenantId.of("demo"), SheetDetails(runtime.clock.instant()))
    val server = App(runtime, { AppInfra(it, storage) }).asServer(Jetty(0)).start()
    Runtime.getRuntime().addShutdownHook(Thread(server::stop))
    println("Fanbelt sheets on http://localhost:${server.port()}")
}

private fun localDatabase(file: File) = file.apply { transaction(SqliteDatabase(this)) { SchemaUtils.create(*SheetTables.all.map(SqliteDocuments::schema).toTypedArray()) } }