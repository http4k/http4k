package org.reekwest.http.contract

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status

object CatchContractBreach : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = {
        try {
            next(it)
        } catch (e: ContractBreach) {
            Response(Status.BAD_REQUEST)
        }
    }
}