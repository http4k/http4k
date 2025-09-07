package workflows

import io.typeflows.github.workflows.steps.marketplace.JavaDistribution.Adopt
import io.typeflows.github.workflows.steps.marketplace.JavaVersion.V21
import io.typeflows.github.workflows.steps.marketplace.SetupJava

object Standards {
    val Java = SetupJava(Adopt, V21)
    val masterBranch = "master"

    val REELEASE_EVENT = "http4k-release"

}
