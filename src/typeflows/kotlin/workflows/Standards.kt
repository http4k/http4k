package workflows

import io.typeflows.github.workflow.step.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflow.step.marketplace.JavaVersion.V21
import io.typeflows.github.workflow.step.marketplace.SetupJava

object Standards {
    val Java = SetupJava(Adopt, V21)
    val MASTER_BRANCH = "master"
    val MAIN_REPO = "http4k/http4k"

    val RELEASE_EVENT = "http4k-release"
}
