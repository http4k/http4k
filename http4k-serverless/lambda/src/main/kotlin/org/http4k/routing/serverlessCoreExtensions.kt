package org.http4k.routing

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.StreamHandler

infix fun String.bind(sh: StreamHandler<Context>): NamedFunctionLoader<Context> = this bind { _: Map<String, String> ->
    sh
}

infix fun String.bind(fn: FunctionLoader<Context>) = NamedFunctionLoader(this, fn)

