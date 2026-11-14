package org.http4k.connect.openfga

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.openfga.model.Assertion
import org.http4k.connect.openfga.model.Assertions
import org.http4k.connect.openfga.model.BatchCheckItem
import org.http4k.connect.openfga.model.ConsistencyPreference.HIGHER_CONSISTENCY
import org.http4k.connect.openfga.model.CorrelationId
import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.OnConflict.IGNORE
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.RelationDefinition.Companion.butNot
import org.http4k.connect.openfga.model.RelationDefinition.Companion.computed
import org.http4k.connect.openfga.model.RelationDefinition.Companion.direct
import org.http4k.connect.openfga.model.RelationDefinition.Companion.from
import org.http4k.connect.openfga.model.RelationDefinition.Companion.intersection
import org.http4k.connect.openfga.model.RelationDefinition.Companion.union
import org.http4k.connect.openfga.model.RelationReference
import org.http4k.connect.openfga.model.StoreId
import org.http4k.connect.openfga.model.StoreName
import org.http4k.connect.openfga.model.TupleFilter
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TupleKeys
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_DELETE
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_WRITE
import org.http4k.connect.openfga.model.TypeDefinition
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.connect.openfga.model.UserTypeFilter
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

interface OpenFgaContract {
    val http: HttpHandler

    val baseUri: Uri get() = Uri.of("http://localhost")

    val openFga get() = OpenFga.Http(baseUri, http)

    @Test
    fun `creates a store`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue()

        assertThat(store.name, equalTo(StoreName.of("orion")))
    }

    @Test
    fun `gets a store`() {
        val created = openFga.createStore(StoreName.of("fetched")).successValue()

        assertThat(openFga.getStore(created.id).successValue().name, equalTo(StoreName.of("fetched")))
    }

    @Test
    fun `lists the stores with a name`() {
        val first = openFga.createStore(StoreName.of("listed")).successValue()
        val second = openFga.createStore(StoreName.of("listed")).successValue()
        openFga.createStore(StoreName.of("unlisted")).successValue()

        val stores = openFga.listStores(StoreName.of("listed")).successValue().stores

        assertThat(stores.map { it.id }.toSet(), equalTo(setOf(first.id, second.id)))
    }

    @Test
    fun `a deleted store is gone`() {
        val store = openFga.createStore(StoreName.of("deleted")).successValue().id

        openFga.deleteStore(store).successValue()

        assertThat(openFga.getStore(store).failureOrNull()?.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `reads a store's authorization models, newest first`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id
        val first = openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user))).successValue().authorization_model_id
        val second = openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user), TypeDefinition.of(doc)))
            .successValue().authorization_model_id

        val models = openFga.readAuthorizationModels(store).successValue().authorization_models

        assertThat(models.map { it.id }, equalTo(listOf(second, first)))
    }

    @Test
    fun `reads an authorization model by id`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id
        val id = openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user))))
            .successValue().authorization_model_id

        val model = openFga.readAuthorizationModel(store, id).successValue().authorization_model

        assertThat(model.type_definitions.map { it.type }, equalTo(listOf(user, doc)))
    }

    @Test
    fun `reads back the assertions written for a model`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id
        val model = openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user))))
            .successValue().authorization_model_id
        val assertions = listOf(Assertion(TupleKey(anne, viewer, budget), true), Assertion(TupleKey(bob, viewer, budget), false))

        openFga.writeAssertions(store, model, assertions).successValue()

        assertThat(openFga.readAssertions(store, model).successValue(), equalTo(Assertions(model, assertions)))
    }

    @Test
    fun `a user written into a relation is allowed it, and no one else is`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(doc, viewer to direct(user))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `checks many tuples at once, answering each by its correlation id`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget))).successValue()

        val results = openFga.batchCheck(
            store, listOf(
                BatchCheckItem(TupleKey(anne, viewer, budget), CorrelationId.of("anne")),
                BatchCheckItem(TupleKey(bob, viewer, budget), CorrelationId.of("bob"))
            )
        ).successValue().result

        assertThat(results.mapValues { it.value.allowed }, equalTo(mapOf(CorrelationId.of("anne") to true, CorrelationId.of("bob") to false)))
    }

    @Test
    fun `contextual tuples grant a relation for that request alone`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        val granted = TupleKey(anne, viewer, budget)

        assertThat(openFga.check(store, granted, contextualTuples = listOf(granted)).successValue().allowed, equalTo(true))
        assertThat(allowed(store, anne, viewer, budget), equalTo(false))
    }

    @Test
    fun `contextual tuples count when listing objects`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))

        val objects = openFga.listObjects(store, doc, viewer, anne, contextualTuples = listOf(TupleKey(anne, viewer, budget)))
            .successValue().objects

        assertThat(objects, equalTo(listOf(budget)))
    }

    @Test
    fun `contextual tuples apply to their own check in a batch`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        val granted = TupleKey(anne, viewer, budget)

        val results = openFga.batchCheck(
            store, listOf(
                BatchCheckItem(granted, CorrelationId.of("with"), TupleKeys(listOf(granted))),
                BatchCheckItem(granted, CorrelationId.of("without"))
            )
        ).successValue().result

        assertThat(results.mapValues { it.value.allowed }, equalTo(mapOf(CorrelationId.of("with") to true, CorrelationId.of("without") to false)))
    }

    @Test
    fun `queries can be pinned to an earlier authorization model`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id
        val earlier = openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user))))
            .successValue().authorization_model_id
        val granted = TupleKey(anne, viewer, budget)
        openFga.write(store, writes = listOf(granted)).successValue()
        openFga.writeAuthorizationModel(store, listOf(TypeDefinition.of(user), TypeDefinition.of(doc, editor to direct(user), viewer to computed(editor))))
            .successValue()

        assertThat(openFga.check(store, granted).successValue().allowed, equalTo(false))
        assertThat(openFga.check(store, granted, authorizationModel = earlier).successValue().allowed, equalTo(true))
        assertThat(openFga.listObjects(store, doc, viewer, anne, authorizationModel = earlier).successValue().objects, equalTo(listOf(budget)))
        assertThat(
            openFga.batchCheck(store, listOf(BatchCheckItem(granted, CorrelationId.of("anne"))), authorizationModel = earlier)
                .successValue().result.mapValues { it.value.allowed },
            equalTo(mapOf(CorrelationId.of("anne") to true))
        )
    }

    @Test
    fun `queries can ask for higher consistency`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        val granted = TupleKey(anne, viewer, budget)
        openFga.write(store, writes = listOf(granted)).successValue()

        assertThat(openFga.check(store, granted, consistency = HIGHER_CONSISTENCY).successValue().allowed, equalTo(true))
        assertThat(openFga.listObjects(store, doc, viewer, anne, consistency = HIGHER_CONSISTENCY).successValue().objects, equalTo(listOf(budget)))
        assertThat(
            openFga.batchCheck(store, listOf(BatchCheckItem(granted, CorrelationId.of("anne"))), consistency = HIGHER_CONSISTENCY)
                .successValue().result.mapValues { it.value.allowed },
            equalTo(mapOf(CorrelationId.of("anne") to true))
        )
        assertThat(openFga.read(store, consistency = HIGHER_CONSISTENCY).successValue().tuples.map { it.key }, equalTo(listOf(granted)))
    }

    @Test
    fun `a wildcard grants a relation to every user of a type`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(doc, viewer to direct(RelationReference(user), RelationReference.wildcard(user)))
        )
        openFga.write(store, writes = listOf(TupleKey(User.of("user:*"), viewer, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(true))
        assertThat(allowed(store, anne, viewer, plan), equalTo(false))
    }

    @Test
    fun `a relation can be computed from another relation on the same object`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(doc, editor to direct(user), viewer to union(direct(user), computed(editor)))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, editor, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, anne, editor, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `a relation can be granted to every member of a group`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(team, member to direct(user)),
            TypeDefinition.of(doc, viewer to direct(RelationReference(user), RelationReference(team, member)))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, member, acme), TupleKey(User.of("team:acme#member"), viewer, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `a relation can be inherited from a related object`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(folder, viewer to direct(user)),
            TypeDefinition.of(doc, parent to direct(folder), viewer to union(direct(user), viewer from parent))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, q3), TupleKey(User.of("folder:q3"), parent, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `a relation can require two others`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(doc, editor to direct(user), member to direct(user), viewer to intersection(computed(editor), computed(member)))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, editor, budget), TupleKey(anne, member, budget), TupleKey(bob, editor, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `a relation can exclude another`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(doc, editor to direct(user), blocked to direct(user), viewer to (computed(editor) butNot computed(blocked)))
        )
        openFga.write(store, writes = listOf(TupleKey(anne, editor, budget), TupleKey(bob, editor, budget), TupleKey(bob, blocked, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(true))
        assertThat(allowed(store, bob, viewer, budget), equalTo(false))
    }

    @Test
    fun `a deleted tuple no longer grants its relation`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget), TupleKey(bob, viewer, budget))).successValue()

        openFga.write(store, deletes = listOf(TupleKey(anne, viewer, budget))).successValue()

        assertThat(allowed(store, anne, viewer, budget), equalTo(false))
        assertThat(allowed(store, bob, viewer, budget), equalTo(true))
    }

    @Test
    fun `writing a tuple that already exists is refused, unless duplicates are ignored`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        val granted = TupleKey(anne, viewer, budget)
        openFga.write(store, writes = listOf(granted)).successValue()

        assertThat(openFga.write(store, writes = listOf(granted)).failureOrNull()?.status, equalTo(BAD_REQUEST))

        openFga.write(store, writes = listOf(granted), onDuplicate = IGNORE).successValue()
        assertThat(read(store, TupleFilter(obj = budget)), equalTo(setOf(granted)))
    }

    @Test
    fun `deleting a tuple that does not exist is refused, unless missing tuples are ignored`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        val missing = TupleKey(anne, viewer, budget)

        assertThat(openFga.write(store, deletes = listOf(missing)).failureOrNull()?.status, equalTo(BAD_REQUEST))

        openFga.write(store, deletes = listOf(missing), onMissing = IGNORE).successValue()
    }

    @Test
    fun `lists the objects a user has a relation with`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(folder, viewer to direct(user)),
            TypeDefinition.of(doc, parent to direct(folder), viewer to union(direct(user), viewer from parent))
        )
        openFga.write(
            store, writes = listOf(
                TupleKey(anne, viewer, budget),
                TupleKey(anne, viewer, q3),
                TupleKey(User.of("folder:q3"), parent, plan),
                TupleKey(bob, viewer, secrets)
            )
        ).successValue()

        val objects = openFga.listObjects(store, doc, viewer, anne).successValue().objects

        assertThat(objects.toSet(), equalTo(setOf(budget, plan)))
    }

    @Test
    fun `streams the objects a user has a relation with`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget), TupleKey(anne, viewer, plan), TupleKey(bob, viewer, secrets)))
            .successValue()

        val objects = openFga.streamedListObjects(store, doc, viewer, anne).successValue()

        assertThat(objects.toSet(), equalTo(setOf(budget, plan)))
    }

    @Test
    fun `lists the users with a relation to an object, of the type asked for`() {
        val store = storeWith(
            TypeDefinition.of(user),
            TypeDefinition.of(team, member to direct(user)),
            TypeDefinition.of(doc, viewer to direct(RelationReference(user), RelationReference.wildcard(user), RelationReference(team, member)))
        )
        openFga.write(
            store, writes = listOf(
                TupleKey(anne, viewer, budget),
                TupleKey(User.of("team:acme#member"), viewer, budget),
                TupleKey(bob, member, acme),
                TupleKey(carol, viewer, plan),
                TupleKey(User.of("user:*"), viewer, secrets)
            )
        ).successValue()

        assertThat(openFga.listUsers(store, budget, viewer, UserTypeFilter(user)).successValue().users.toSet(), equalTo(setOf(anne, bob)))
        assertThat(
            openFga.listUsers(store, budget, viewer, UserTypeFilter(team, member)).successValue().users,
            equalTo(listOf(User.of("team:acme#member")))
        )
        assertThat(openFga.listUsers(store, secrets, viewer, UserTypeFilter(user)).successValue().users, equalTo(listOf(User.of("user:*"))))
    }

    @Test
    fun `reads the tuples matching a filter`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, editor to direct(user), viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget), TupleKey(bob, viewer, budget), TupleKey(anne, editor, plan))).successValue()

        assertThat(
            read(store, TupleFilter(obj = budget)),
            equalTo(setOf(TupleKey(anne, viewer, budget), TupleKey(bob, viewer, budget)))
        )
        assertThat(
            read(store, TupleFilter(user = anne, obj = FgaObject.of("doc:"))),
            equalTo(setOf(TupleKey(anne, viewer, budget), TupleKey(anne, editor, plan)))
        )
    }

    @Test
    fun `reads changes in order, and from where a previous read stopped`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(doc, viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget))).successValue()
        openFga.write(store, deletes = listOf(TupleKey(anne, viewer, budget))).successValue()

        val first = openFga.readChanges(store).successValue()

        assertThat(
            first.changes.map { it.operation to it.tuple_key },
            equalTo(listOf(TUPLE_OPERATION_WRITE to TupleKey(anne, viewer, budget), TUPLE_OPERATION_DELETE to TupleKey(anne, viewer, budget)))
        )

        openFga.write(store, writes = listOf(TupleKey(bob, viewer, budget))).successValue()

        val next = openFga.readChanges(store, first.continuation_token).successValue()

        assertThat(next.changes.map { it.operation to it.tuple_key }, equalTo(listOf(TUPLE_OPERATION_WRITE to TupleKey(bob, viewer, budget))))
    }

    @Test
    fun `reads only the changes to one type`() {
        val store = storeWith(TypeDefinition.of(user), TypeDefinition.of(folder, viewer to direct(user)), TypeDefinition.of(doc, viewer to direct(user)))
        openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget), TupleKey(anne, viewer, q3))).successValue()

        val changes = openFga.readChanges(store, type = folder).successValue().changes

        assertThat(changes.map { it.tuple_key }, equalTo(listOf(TupleKey(anne, viewer, q3))))
    }

    @Test
    fun `a store cannot be checked before it has an authorization model`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id

        val result = openFga.check(store, TupleKey(anne, viewer, budget))

        assertThat(result.failureOrNull()?.status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `a store cannot be written to before it has an authorization model`() {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id

        val result = openFga.write(store, writes = listOf(TupleKey(anne, viewer, budget)))

        assertThat(result.failureOrNull()?.status, equalTo(BAD_REQUEST))
    }

    private fun read(store: StoreId, filter: TupleFilter) =
        openFga.read(store, filter).successValue().tuples.map { it.key }.toSet()

    private fun storeWith(vararg types: TypeDefinition): StoreId {
        val store = openFga.createStore(StoreName.of("orion")).successValue().id
        openFga.writeAuthorizationModel(store, types.toList()).successValue()
        return store
    }

    private fun allowed(store: StoreId, user: User, relation: Relation, obj: FgaObject) =
        openFga.check(store, TupleKey(user, relation, obj)).successValue().allowed
}

private val user = TypeName.of("user")
private val doc = TypeName.of("doc")
private val team = TypeName.of("team")
private val folder = TypeName.of("folder")
private val parent = Relation.of("parent")
private val q3 = FgaObject.of("folder:q3")
private val member = Relation.of("member")
private val acme = FgaObject.of("team:acme")
private val anne = User.of("user:anne")
private val bob = User.of("user:bob")
private val carol = User.of("user:carol")
private val editor = Relation.of("editor")
private val blocked = Relation.of("blocked")
private val viewer = Relation.of("viewer")
private val budget = FgaObject.of("doc:budget")
private val plan = FgaObject.of("doc:plan")
private val secrets = FgaObject.of("doc:secrets")
