@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openfga.model

import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TypeDefinition(
    val type: TypeName,
    val relations: Map<Relation, Userset> = emptyMap(),
    val metadata: Metadata? = null
) {
    companion object {
        fun of(type: TypeName, vararg relations: Pair<Relation, RelationDefinition>) = TypeDefinition(
            type,
            relations.associate { (relation, definition) -> relation to definition.rewrite },
            Metadata(relations.associate { (relation, definition) -> relation to RelationMetadata(definition.directlyRelated) })
                .takeIf { relations.isNotEmpty() }
        )
    }
}

@JsonSerializable
data class Userset(
    @JsonProperty("this")
    val direct: DirectUserset? = null,
    val computedUserset: ObjectRelation? = null,
    val tupleToUserset: TupleToUserset? = null,
    val union: Usersets? = null,
    val intersection: Usersets? = null,
    val difference: Difference? = null
)

@JsonSerializable
data class Difference(val base: Userset, val subtract: Userset)

@JsonSerializable
data class TupleToUserset(val tupleset: ObjectRelation, val computedUserset: ObjectRelation)

@JsonSerializable
data class ObjectRelation(@JsonProperty("object") val obj: String = "", val relation: Relation)

@JsonSerializable
data class Usersets(val child: List<Userset>)

@JsonSerializable
data object DirectUserset

@JsonSerializable
data class Metadata(val relations: Map<Relation, RelationMetadata> = emptyMap())

@JsonSerializable
data class RelationMetadata(val directly_related_user_types: List<RelationReference> = emptyList())

@JsonSerializable
data class RelationReference(val type: TypeName, val relation: Relation? = null, val wildcard: Wildcard? = null) {
    companion object {
        fun wildcard(type: TypeName) = RelationReference(type, wildcard = Wildcard)
    }
}

@JsonSerializable
data object Wildcard

data class RelationDefinition(val rewrite: Userset, val directlyRelated: List<RelationReference>) {
    companion object {
        fun direct(vararg types: TypeName) = direct(*types.map { RelationReference(it) }.toTypedArray())

        fun direct(vararg references: RelationReference) = RelationDefinition(Userset(direct = DirectUserset), references.toList())

        fun computed(relation: Relation) = RelationDefinition(Userset(computedUserset = ObjectRelation(relation = relation)), emptyList())

        infix fun Relation.from(tupleset: Relation) = RelationDefinition(
            Userset(tupleToUserset = TupleToUserset(ObjectRelation(relation = tupleset), ObjectRelation(relation = this))),
            emptyList()
        )

        fun union(vararg definitions: RelationDefinition) = RelationDefinition(
            Userset(union = Usersets(definitions.map { it.rewrite })),
            definitions.flatMap { it.directlyRelated }
        )

        fun intersection(vararg definitions: RelationDefinition) = RelationDefinition(
            Userset(intersection = Usersets(definitions.map { it.rewrite })),
            definitions.flatMap { it.directlyRelated }
        )

        infix fun RelationDefinition.butNot(subtract: RelationDefinition) = RelationDefinition(
            Userset(difference = Difference(rewrite, subtract.rewrite)),
            directlyRelated + subtract.directlyRelated
        )
    }
}
