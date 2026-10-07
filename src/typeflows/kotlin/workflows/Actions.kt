package workflows

import io.typeflows.github.workflow.step.marketplace.JavaVersion.V25
import io.typeflows.github.workflow.step.marketplace.SetupJava
import io.typeflows.github.workflow.step.marketplace.Version
import org.http4k.typeflows.GithubActionConstants.JDK
import org.http4k.typeflows.GithubActionConstants.SETUP_JAVA

/**
 * GitHub Actions pinned to full commit SHAs (supply-chain hardening).
 */
object Actions {
    const val COSIGN_INSTALLER = "sigstore/cosign-installer@6f9f17788090df1f26f669e9d70d6ae9567deba6" // v4.1.2
    const val JUNIT_REPORT = "mikepenz/action-junit-report@6ef3fdcfcd5f0b5d14df3e659ef19b8c6820f93d" // v6.6.1
    const val CODECOV = "codecov/codecov-action@303a32d7a59b442fa8d48b6a1cc6825c09c847a5" // v7.1.1
    const val DEPENDENCY_REVIEW = "actions/dependency-review-action@a1d282b36b6f3519aa1f3fc636f609c47dddb294" // v5.0.0
    const val CREATE_GITHUB_APP_TOKEN = "actions/create-github-app-token@bcd2ba49218906704ab6c1aa796996da409d3eb1" // v3.2.0
    const val DEPENDENCY_SUBMISSION = "gradle/actions/dependency-submission@3f5f9adaf7d9fecd50b5935e54106014257a94e6" // v6.4.0
    const val WRAPPER_VALIDATION = "gradle/actions/wrapper-validation@3f5f9adaf7d9fecd50b5935e54106014257a94e6" // v6.4.0
    const val SCORECARD = "ossf/scorecard-action@2d1146689b8cda280b9bc96326124645441f03bc" // v2.4.4
    const val UPLOAD_SARIF = "github/codeql-action/upload-sarif@24c54180a607b1449ed407dd24f251e4e9147c8d" // v4.38.3
    const val CODEQL_INIT = "github/codeql-action/init@24c54180a607b1449ed407dd24f251e4e9147c8d" // v4.38.3
    const val CODEQL_ANALYZE = "github/codeql-action/analyze@24c54180a607b1449ed407dd24f251e4e9147c8d" // v4.38.3
    const val ADD_AND_COMMIT = "EndBug/add-and-commit@cc9c08ba6c8df3b93a8f2db63e89b98368ae2ae8" // v11.1.1
    const val BUILDNOTE = "buildnote/action@dccb92269d3f9a2515ad63e03d45af686ce3febd" // v1.2.0
    const val GITHUB_PUSH = "ad-m/github-push-action@881a6320fdb16eb5318c5054f31c218aec2b324c" // v1.3.0
    const val CONFIGURE_AWS = "aws-actions/configure-aws-credentials@e1253824e5c10ff9df46874f81ed3ec929e19cfd" // v6.3.0
    const val UPLOAD_ARTIFACT = "actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9" // v7.0.2
    const val DOWNLOAD_ARTIFACT = "actions/download-artifact@9000827ccba6bdab643e8b6fd33ac0654aef8333" // v8.0.2
    const val REPO_SYNC_PULL_REQUEST = "repo-sync/pull-request@7e79a9f5dc3ad0ce53138f01df2fad14a04831c5" // v2.12.1

    // First-party / marketplace actions
    val CREATE_RELEASE = Version.sha("0cb9c9b65d5d1901c1f53e5e66eaf4afd303e70e") // actions/create-release v1.1.4

    val SetupJavaAction = SetupJava(JDK, V25, SETUP_JAVA)

}
