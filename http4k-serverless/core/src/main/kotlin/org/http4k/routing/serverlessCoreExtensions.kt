package org.http4k.routing

import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import java.io.InputStream

infix fun <Ctx> String.bind(sh: FnHandler<InputStream, Ctx, InputStream>) = this bind { _: Map<String, String> -> sh }

/**
 * Bind a function name (or regex pattern) to a particular function loader. This is useful
 * for matching several different functions in a single deployed executable (eg. when a custom
 * runtime is used).
 */
infix fun <Ctx> String.bind(fn: FnLoader<Ctx>) = NamedFnLoader(toRegex(), fn)

class NamedFnLoader<Ctx>(val nameRegex: Regex, loader: FnLoader<Ctx>) : FnLoader<Ctx> by loader
