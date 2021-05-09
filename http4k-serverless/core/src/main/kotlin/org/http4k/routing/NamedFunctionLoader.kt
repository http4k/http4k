package org.http4k.routing

import org.http4k.serverless.FunctionLoader

class NamedFunctionLoader<Ctx>(val name: String, loader: FunctionLoader<Ctx>) : FunctionLoader<Ctx> by loader
