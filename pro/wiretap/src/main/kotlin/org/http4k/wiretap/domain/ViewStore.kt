package org.http4k.wiretap.domain

import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import java.util.concurrent.CopyOnWriteArrayList

data class View(
    val id: ViewId,
    val name: String,
    val builtIn: Boolean = false,
    val filter: TransactionFilter = TransactionFilter()
)

typealias ViewId = Long

interface ViewStore {
    fun list(): List<View>
    fun add(name: String, filter: TransactionFilter): View
    fun update(view: View)
    fun remove(id: ViewId)

    companion object {
        val defaultViews = listOf(
            View(id = -3, name = "All", builtIn = true),
            View(
                id = -2,
                name = Inbound.name,
                builtIn = true,
                filter = TransactionFilter(direction = Inbound)
            ),
            View(
                id = -1,
                name = Outbound.name,
                builtIn = true,
                filter = TransactionFilter(direction = Outbound)
            )
        )

    fun InMemory(initial: List<View> = defaultViews): ViewStore = object : ViewStore {
            private val views = CopyOnWriteArrayList(initial)

            override fun list() = views.toList()

            override fun add(name: String, filter: TransactionFilter): View {
                val view = View(id = (views.size + 1).toLong(), name = name, filter = filter)
                views.add(view)
                return view
            }

            override fun update(view: View) {
                val index = views.indexOfFirst { it.id == view.id && !it.builtIn }
                if (index >= 0) views[index] = view
            }

            override fun remove(id: ViewId) {
                views.removeIf { it.id == id && !it.builtIn }
            }
        }
    }
}
