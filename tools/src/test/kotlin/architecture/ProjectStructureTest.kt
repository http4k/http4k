package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.classes
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ProjectStructureTest {

    @Test
    fun `tests requiring network implement PortBasedTest`() {
        val portRequiredClasses = Konsist.scopeFromTest()
            .files
            .filter { file ->
                file.hasImport {
                    it.hasNameStartingWith("org.http4k.server.") || it.hasNameStartingWith("org.http4k.client.")
                }
            }
            .filter { it.packagee?.name != "tools" }
            .classes()

        portRequiredClasses
            .fold(listOf<KoClassDeclaration>(), { acc, next ->
                when {
                    next.hasNameEndingWith("Test") -> acc + next
                    next.hasParentWithName(portRequiredClasses.map(KoClassDeclaration::name)) -> acc + next
                    else -> acc
                }
            })
            .assertTrue { it.hasParentInterfaceWithName("PortBasedTest", indirectParents = true) }
    }
}
