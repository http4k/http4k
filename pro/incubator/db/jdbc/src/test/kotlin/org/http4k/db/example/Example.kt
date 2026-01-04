package org.http4k.db.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.db.InMemoryTransactor
import org.http4k.db.jdbc.DataSourceTransactor
import org.http4k.db.TransactionPerRequestFilter
import org.http4k.db.transactionResource
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.opentest4j.TestAbortedException
import java.sql.Connection

interface MessageRepository {
    fun getMessage(id: Int): String?
    fun saveMessage(id: Int, message: String): Int
}

class InMemoryMessageRepository : MessageRepository {
    private val messages = mutableMapOf<Int, String>()

    override fun getMessage(id: Int): String? = messages[id]

    override fun saveMessage(id: Int, message: String): Int {
        messages[id] = message
        return id
    }
}

private fun createPostgresDataSource() = try {
    HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "mysecretpassword"
        jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
    })
} catch (e: Exception) {
    throw TestAbortedException("Postgres not available")
}

private fun initialisePostgresDatabase(connection: Connection) {
    connection.createStatement().use {
        it.execute(
            """DROP SCHEMA public CASCADE; 
                CREATE SCHEMA public; 
                GRANT ALL ON SCHEMA public TO postgres;
                GRANT ALL ON SCHEMA public TO public;
                
                create table messages(
                    id integer default 0 PRIMARY KEY,
                    message varchar(255) not null
                );
              """
        )
    }
}

class PostgresMessageRepository(private val connection: Connection) : MessageRepository {
    override fun getMessage(id: Int): String? {
        connection.prepareStatement("select message from messages where id = ?").use {
            it.setInt(1, id)
            it.executeQuery().use { result ->
                result.next()
                return result.getString("message")
            }
        }
    }

    override fun saveMessage(id: Int, message: String): Int {
        connection.prepareStatement("insert into messages (id, message) values (?, ?)  on conflict(id) do update set message=?")
            .use {
                it.setInt(1, id)
                it.setString(2, message)
                it.setString(3, message)
                it.execute()
            }
        return id
    }
}

private val messageId = Path.int().of("id")

fun main() {
    initialisePostgresDatabase(createPostgresDataSource().connection)

    val transactor = DataSourceTransactor(
        createPostgresDataSource(),
        ::PostgresMessageRepository
    )

    // or use in-memory for testing
    // val transactor = InMemoryTransactor(::InMemoryMessageRepository)

    val app = routes(
        "/message/{id}" bind Method.GET to
            { request: Request ->
                val repository: MessageRepository = request.transactionResource()

                val message = repository.getMessage(messageId(request))

                if (message == null) Response.Companion(Status.NOT_FOUND)
                    .body("Message not found") else Response.Companion(OK).body(message)
            },
        "/message/{id}" bind Method.POST to
            { request: Request ->
                val repository: MessageRepository = request.transactionResource()

                repository.saveMessage(messageId(request), request.bodyString())

                Response.Companion(OK)
            })

    val server = Filter.NoOp
        .then(TransactionPerRequestFilter(transactor)) // injects the AccountRepository into the request
        .then(app)

    server(Request.Companion(Method.POST, "/message/1").body("Hello World"))
    server(Request.Companion(Method.GET, "/message/1")).let(::println) // prints Response(OK, body=Hello World)
}
