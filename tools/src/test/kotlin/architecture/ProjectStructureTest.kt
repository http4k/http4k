package architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.classes
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ProjectStructureTest {

    @Test
    fun `tests requiring network implement PortBasedTest`() = runBlocking {
        val portRequiredClasses = Konsist.scopeFromTest()
            .files
            .filter { file ->
                file.hasImport {
                    it.hasNameStartingWith("org.http4k.server.") || it.hasNameStartingWith("org.http4k.client.")
                } || file.hasPackage("org.http4k.server") || file.hasPackage("org.http4k.client")
            }
            .classes()

        val portClassNames = portRequiredClasses.map(KoClassDeclaration::name)

        Konsist.scopeFromTest()
            .files
            .classes()
            .filter {
                it.hasNameEndingWith("Test") &&
                    (it.name in portClassNames || it.hasParentWithName(portClassNames, indirectParents = true))
            }
            .assertTrue {
                it.hasParentInterfaceWithName("PortBasedTest", indirectParents = true)
                    || it.hasParentInterfaceWithName("InMemoryTest", indirectParents = true)
            }
    }

    @Test
    fun `tests class names end with Test or Contract`() = runBlocking {
        Konsist.scopeFromTest()
            .files
            .classes()
            .filter { klass ->
                klass.hasFunction(true) { it.hasAnnotation { it.hasNameEndingWith("Test") } }
                    && !klass.hasAnnotation { it.hasNameEndingWith("Nested") }
            }
            .assertTrue { it.hasNameEndingWith("Test") || it.hasNameEndingWith("Contract") }
    }
}
