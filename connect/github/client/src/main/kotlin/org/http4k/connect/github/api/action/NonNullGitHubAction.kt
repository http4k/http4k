package org.http4k.connect.github.api.action

import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.github.GitHubMoshi
import org.http4k.connect.github.api.GitHubAction
import kotlin.reflect.KClass

abstract class NonNullGitHubAction<R : Any>(clazz: KClass<R>) : NonNullAutoMarshalledAction<R>(clazz, GitHubMoshi),
    GitHubAction<R>

