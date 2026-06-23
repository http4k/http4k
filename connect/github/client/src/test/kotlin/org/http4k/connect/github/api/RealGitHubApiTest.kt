package org.http4k.connect.github.api

import org.http4k.client.JavaHttpClient
import org.http4k.connect.github.GitHubToken
import org.http4k.filter.debug
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach

class RealGitHubApiTest : GitHubApiContract(
    tokenFn = { GitHubToken.parse(System.getenv("GITHUB_TOKEN")!!) },
    http = JavaHttpClient().debug()
), PortBasedTest {
    @BeforeEach
    fun checkPrerequisites() {
        assumeTrue(System.getenv("GITHUB_TOKEN") != null, "GITHUB_TOKEN environment variable not found")
    }
}
