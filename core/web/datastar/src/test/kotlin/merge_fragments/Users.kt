package merge_fragments

class Users {
    private var count = 0

    private val contacts = mutableListOf<User.Saved>()

    init {
        add(User.New("Joe", "Bloggs", "asdasd@gmail.com"))
        add(User.New("Bob", "Smith", "smith@gmail.com"))
    }

    fun list() = contacts.sortedBy { it.id }

    fun get(id: Int) = contacts.find { it.id == id }

    fun delete(id: Int): Unit? =
        if (contacts.removeIf { it.id == id }) Unit else null

    fun add(new: User.New) = count++.also {
        contacts.add(new.withId(count++))
    }

    fun update(patch: User.Saved) = when {
        contacts.removeIf { it.id == patch.id } -> {
            contacts.add(patch)
            patch
        }

        else -> null
    }
}
