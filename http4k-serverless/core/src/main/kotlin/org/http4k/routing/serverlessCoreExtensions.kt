package org.http4k.routing

import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import java.io.InputStream

infix fun <Ctx> String.bind(sh: FnHandler<InputStream, Ctx, InputStream>) = this bind { _: Map<String, String> -> sh }

infix fun <Ctx> String.bind(fn: FnLoader<Ctx>) = NamedFnLoader(this, fn)

class NamedFnLoader<Ctx>(val name: String, loader: FnLoader<Ctx>) : FnLoader<Ctx> by loader
