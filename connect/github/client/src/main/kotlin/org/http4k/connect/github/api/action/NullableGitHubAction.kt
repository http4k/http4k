package org.http4k.connect.github.api.action

import org.http4k.connect.NullableAutoMarshalledAction
import org.http4k.connect.github.GitHubMoshi
import org.http4k.connect.github.api.GitHubAction
import kotlin.reflect.KClass

abstract class NullableGitHubAction<R : Any>(clazz: KClass<R>) : NullableAutoMarshalledAction<R>(clazz, GitHubMoshi),
    GitHubAction<R?>
