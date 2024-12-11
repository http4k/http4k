package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.classes
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ProjectStructureTest {

    @Test
    fun `tests requiring network implement PortBasedTest`() {
        Konsist.scopeFromTest()
            .files
            .filter { file ->
                file.hasImport {
                    it.hasNameStartingWith("org.http4k.server.") ||
                        it.hasNameStartingWith("org.http4k.client.")
                }
            }
            .classes()
            .filter { it.hasNameEndingWith("Test") }
            .assertTrue { it.hasParentInterfaceWithName("PortBasedTest", indirectParents = true) }
    }
}
