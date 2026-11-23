package todo

class TodoDomainTest : TodoContract() {
    override val driver = DomainTodoDriver()
}