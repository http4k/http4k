package workflows

/**
 * Third-party GitHub Actions pinned to full commit SHAs (supply-chain hardening).
 */
object Actions {
    const val COSIGN_INSTALLER = "sigstore/cosign-installer@c56c2d3e59e4281cc41dea2217323ba5694b171e" // v3.8.0
    const val JUNIT_REPORT = "mikepenz/action-junit-report@3585e9575db828022551b4231f165eb59a0e74e3" // v5.6.2
    const val DEPENDENCY_SUBMISSION = "gradle/actions/dependency-submission@ed408507eac070d1f99cc633dbcf757c94c7933a" // v4
    const val WRAPPER_VALIDATION = "gradle/actions/wrapper-validation@ed408507eac070d1f99cc633dbcf757c94c7933a" // v4
    const val ADD_AND_COMMIT = "EndBug/add-and-commit@a94899bca583c204427a224a7af87c02f9b325d5" // v9
    const val BUILDNOTE = "buildnote/action@dccb92269d3f9a2515ad63e03d45af686ce3febd" // v1.2.0
    const val GITHUB_PUSH = "ad-m/github-push-action@881a6320fdb16eb5318c5054f31c218aec2b324c" // v1.3.0
}
