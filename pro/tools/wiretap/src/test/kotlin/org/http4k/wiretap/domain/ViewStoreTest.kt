package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ViewStoreTest {

    @Test
    fun `add creates view with name and filter and returns it`() {
        val store = ViewStore.InMemory(emptyList())
        val filter = TransactionFilter(path = "/api")

        val view = store.add("My View", filter)

        assertThat(view.name, equalTo("My View"))
        assertThat(view.filter, equalTo(filter))
        assertThat(store.list(), equalTo(listOf(view)))
    }

    @Test
    fun `add generates unique ids`() {
        val store = ViewStore.InMemory(emptyList())

        val view1 = store.add("View 1", TransactionFilter())
        val view2 = store.add("View 2", TransactionFilter())

        assertThat(view1.id != view2.id, equalTo(true))
    }
}
