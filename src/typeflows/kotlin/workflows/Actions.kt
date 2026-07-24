package workflows

import io.typeflows.github.workflow.step.marketplace.Version

/**
 * GitHub Actions pinned to full commit SHAs (supply-chain hardening).
 */
object Actions {
    const val COSIGN_INSTALLER = "sigstore/cosign-installer@c56c2d3e59e4281cc41dea2217323ba5694b171e" // v3.8.0
    const val JUNIT_REPORT = "mikepenz/action-junit-report@3585e9575db828022551b4231f165eb59a0e74e3" // v5.6.2
    const val CODECOV = "codecov/codecov-action@0fb7174895f61a3b6b78fc075e0cd60383518dac" // v5.5.5
    const val DEPENDENCY_REVIEW = "actions/dependency-review-action@2031cfc080254a8a887f58cffee85186f0e49e48" // v4.9.0
    const val DEPENDENCY_SUBMISSION = "gradle/actions/dependency-submission@ed408507eac070d1f99cc633dbcf757c94c7933a" // v4
    const val WRAPPER_VALIDATION = "gradle/actions/wrapper-validation@ed408507eac070d1f99cc633dbcf757c94c7933a" // v4
    const val SCORECARD = "ossf/scorecard-action@4eaacf0543bb3f2c246792bd56e8cdeffafb205a" // v2.4.3
    const val UPLOAD_SARIF = "github/codeql-action/upload-sarif@e4fba868fa4b1b91e1fdab776edc8cfbe6e9fb81" // v4
    const val CODEQL_INIT = "github/codeql-action/init@e4fba868fa4b1b91e1fdab776edc8cfbe6e9fb81" // v4
    const val CODEQL_ANALYZE = "github/codeql-action/analyze@e4fba868fa4b1b91e1fdab776edc8cfbe6e9fb81" // v4
    const val ADD_AND_COMMIT = "EndBug/add-and-commit@a94899bca583c204427a224a7af87c02f9b325d5" // v9
    const val BUILDNOTE = "buildnote/action@dccb92269d3f9a2515ad63e03d45af686ce3febd" // v1.2.0
    const val GITHUB_PUSH = "ad-m/github-push-action@881a6320fdb16eb5318c5054f31c218aec2b324c" // v1.3.0
    const val CONFIGURE_AWS = "aws-actions/configure-aws-credentials@b47578312673ae6fa5b5096b330d9fbac3d116df" // v4.2.1

    // First-party / marketplace actions
    val CREATE_RELEASE = Version.sha("0cb9c9b65d5d1901c1f53e5e66eaf4afd303e70e") // actions/create-release v1.1.4
}
