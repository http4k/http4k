package org.http4k.connect.amazon.lambda

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.connect.amazon.kClass

class DeleteFunction(private val function: Function) : LambdaAction<Unit>(kClass()) {
    override fun toRequest(): Request = Request(Method.DELETE, Uri.of("/2015-03-31/functions/${function.value}"))
}
