package org.http4k.fanbelt.testing.example

import org.http4k.fanbelt.testing.TestTenant

class DomainGreetingDriver : GreetingDriver {
    private val tenant = TestTenant()

    override fun visitor(name: String): Visitor = object : Visitor {
        private val me = PersonName.of(name)

        override fun watches() {
            sees()
        }

        override fun says(text: String) = tenant { execute(GreetingFeature.say, me, text) }

        override fun clears() = tenant { execute(GreetingFeature.clear, me, Unit) }

        override fun sees() = tenant { read(GreetingFeature.greeting)?.text }
    }
}

class GreetingDomainTest : GreetingContract {
    override val driver = DomainGreetingDriver()
}
