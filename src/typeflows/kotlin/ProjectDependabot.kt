import io.typeflows.github.dependabot.DepdendabotSchedule
import io.typeflows.github.dependabot.Dependabot
import io.typeflows.github.dependabot.PackageEcosystem
import io.typeflows.github.dependabot.ScheduleInterval
import io.typeflows.github.dependabot.Update
import io.typeflows.util.Builder

class ProjectDependabot : Builder<Dependabot> {
    override fun build() = Dependabot {
        updates += Update(PackageEcosystem.GitHubActions) {
            directory = "/"
            schedule = DepdendabotSchedule(ScheduleInterval.Monthly)
        }
    }
}
