package org.reekwest.http.core.contract

import org.reekwest.http.core.*

object Query : Spec<Request, String>("query", Request::queries) {

    fun int() = this.map(String::toInt)
}
