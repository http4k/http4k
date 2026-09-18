package org.http4k.connect.typesafe

class FakeTypeSafeTest : TypeSafeContract {
    override val typeSafe = FakeTypeSafe().client()
}
