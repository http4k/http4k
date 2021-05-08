package org.http4k.routing

import org.http4k.serverless.FunctionLoader
import org.http4k.serverless.StreamHandler

infix fun <Ctx> String.bind(sh: StreamHandler<Ctx>): NamedFunctionLoader<Ctx> = this bind { _: Map<String, String> ->
    sh
}

infix fun <Ctx> String.bind(fn: FunctionLoader<Ctx>) = NamedFunctionLoader(this, fn)

class NamedFunctionLoader<Ctx>(val name: String, loader: FunctionLoader<Ctx>) : FunctionLoader<Ctx> by loader
