package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.db.testing.AccountRepository
import org.http4k.db.testing.PlainSqlAccountRepository
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

fun main() {
    val transactor = DataSourceTransactor(createDataSource(), ::PlainSqlAccountRepository)

    val app = routes(
        "/balance/{account}" bind GET to
            { req: Request ->
                val repository: AccountRepository = req.transactionResource() // extracts the AccountRepository fom the request

                val balance = repository.getBalance(req.path("account")!!)
                Response(OK).body(balance.toString())
            })

    val server = Filter.NoOp
        .then(TransactionPerRequestFilter(transactor)) // injects the AccountRepository into the request
        .then(app)

    server(Request(GET, "/balance/DEPOSITS")).let(::println)
}
