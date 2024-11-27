package org.http4k.connect.github

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.github.model.Branch
import org.http4k.connect.github.model.CommitSha
import org.http4k.connect.github.model.Owner
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object GitHubMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(GitHubJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .value(Branch)
        .value(CommitSha)
        .value(Owner)
        .done()
)

@KotshiJsonAdapterFactory
object GitHubJsonAdapterFactory : JsonAdapter.Factory by KotshiGitHubJsonAdapterFactory
