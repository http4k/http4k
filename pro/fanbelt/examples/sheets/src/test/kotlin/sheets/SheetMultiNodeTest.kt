package sheets

import org.http4k.fanbelt.testing.InMemoryCluster

class SheetMultiNodeTest : SheetContract() {
    private val cluster = InMemoryCluster()

    override val driver = UiSheetDriver(cluster.runtime.scheduler, cluster.storage, List(2) { App(cluster.runtime, cluster.node()) })
}