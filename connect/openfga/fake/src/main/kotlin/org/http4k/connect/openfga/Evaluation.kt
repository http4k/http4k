package org.http4k.connect.openfga

import org.http4k.connect.openfga.model.FgaObject
import org.http4k.connect.openfga.model.Relation
import org.http4k.connect.openfga.model.TupleKey
import org.http4k.connect.openfga.model.TupleKeys
import org.http4k.connect.openfga.model.TypeDefinition
import org.http4k.connect.openfga.model.TypeName
import org.http4k.connect.openfga.model.User
import org.http4k.connect.openfga.model.UserTypeFilter
import org.http4k.connect.openfga.model.Userset

class Evaluation(private val types: List<TypeDefinition>, private val tuples: List<TupleKey>) {
    fun with(contextual: TupleKeys?) = Evaluation(types, tuples + contextual?.tuple_keys.orEmpty())

    fun check(user: User, relation: Relation, obj: FgaObject): Boolean =
        types.find { it.type == obj.type }?.relations?.get(relation)
            ?.let { allows(it, user, relation, obj) }
            ?: false

    fun listObjects(type: TypeName, relation: Relation, user: User) =
        tuples.map { it.obj }.distinct().filter { it.type == type && check(user, relation, it) }

    fun listUsers(obj: FgaObject, relation: Relation, filter: UserTypeFilter): List<User> {
        val withoutWildcards = Evaluation(types, tuples.filterNot { it.user.isWildcard })
        return tuples.map { it.user }.distinct()
            .filter { it.matches(filter) }
            .filter { if (it.isWildcard) check(it, relation, obj) else withoutWildcards.check(it, relation, obj) }
    }

    private fun User.matches(filter: UserTypeFilter): Boolean {
        val (subject, userRelation) = (value.split('#') + "").take(2)
        return subject.substringBefore(':') == filter.type.value && userRelation == filter.relation?.value.orEmpty()
    }

    private fun directly(user: User, relation: Relation, obj: FgaObject) =
        tuples.filter { it.relation == relation && it.obj == obj }
            .any { it.user == user || it.user.isWildcardFor(user) || it.user.includes(user) }

    private val User.isWildcard get() = value.endsWith(":*")

    private fun User.isWildcardFor(user: User) = value == "${user.value.substringBefore(':')}:*"

    private fun User.includes(user: User): Boolean {
        val group = value.split('#')
        return group.size == 2 && check(user, Relation.of(group[1]), FgaObject.of(group[0]))
    }

    private fun allows(rewrite: Userset, user: User, relation: Relation, obj: FgaObject): Boolean {
        val computed = rewrite.computedUserset
        val inherited = rewrite.tupleToUserset
        val union = rewrite.union
        val intersection = rewrite.intersection
        val difference = rewrite.difference
        return when {
            rewrite.direct != null -> directly(user, relation, obj)

            computed != null -> check(user, computed.relation, obj)

            inherited != null -> tuples.filter { it.relation == inherited.tupleset.relation && it.obj == obj }
                .any { check(user, inherited.computedUserset.relation, FgaObject.of(it.user.value)) }

            union != null -> union.child.any { allows(it, user, relation, obj) }

            intersection != null -> intersection.child.all { allows(it, user, relation, obj) }

            difference != null -> allows(difference.base, user, relation, obj) && !allows(difference.subtract, user, relation, obj)

            else -> false
        }
    }
}

internal val FgaObject.type get() = TypeName.of(value.substringBefore(':'))
