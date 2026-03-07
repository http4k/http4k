package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

interface ViewStoreContract {

    val store: ViewStore

    @Test
    fun `add creates view with name and filter and returns it`() {
        val filter = TransactionFilter(path = "/api")

        val view = store.add("My View", filter)

        assertThat(view.name, equalTo("My View"))
        assertThat(view.filter, equalTo(filter))
        assertThat(store.list().any { it.id == view.id }, equalTo(true))
    }

    @Test
    fun `add generates unique ids`() {
        val view1 = store.add("View 1", TransactionFilter())
        val view2 = store.add("View 2", TransactionFilter())

        assertThat(view1.id != view2.id, equalTo(true))
    }

    @Test
    fun `list returns default views`() {
        val views = store.list()
        assertThat(views.size, equalTo(ViewStore.defaultViews.size))
    }

    @Test
    fun `update modifies existing view`() {
        val view = store.add("Test", TransactionFilter())
        val newFilter = TransactionFilter(path = "/updated")
        store.update(view.copy(filter = newFilter))

        val updated = store.list().find { it.id == view.id }
        assertThat(updated?.filter, equalTo(newFilter))
    }

    @Test
    fun `remove deletes view`() {
        val view = store.add("Test", TransactionFilter())
        val sizeBefore = store.list().size

        store.remove(view.id)

        assertThat(store.list().size, equalTo(sizeBefore - 1))
        assertThat(store.list().none { it.id == view.id }, equalTo(true))
    }

    @Test
    fun `remove nonexistent is no-op`() {
        val sizeBefore = store.list().size
        store.remove(9999)
        assertThat(store.list().size, equalTo(sizeBefore))
    }

    @Test
    fun `built-in views are present`() {
        val builtIn = store.list().filter { it.builtIn }
        assertThat(builtIn.size, equalTo(3))
        assertThat(builtIn.map { it.name }.toSet(), equalTo(setOf("All", "Inbound", "Outbound")))
    }
}
