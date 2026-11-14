package org.http4k.connect.openfga

import org.http4k.connect.openfga.model.Assertion
import org.http4k.connect.openfga.model.AuthorizationModel
import org.http4k.connect.openfga.model.AuthorizationModelId
import org.http4k.connect.openfga.model.Store
import org.http4k.connect.openfga.model.Tuple
import org.http4k.connect.openfga.model.TupleChange
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_DELETE
import org.http4k.connect.openfga.model.TupleOperation.TUPLE_OPERATION_WRITE

data class FgaStore(
    val store: Store,
    val models: List<AuthorizationModel> = emptyList(),
    val changes: List<TupleChange> = emptyList(),
    val assertions: Map<AuthorizationModelId, List<Assertion>> = emptyMap()
) {
    val tuples
        get() = changes.fold(emptyList<Tuple>()) { tuples, change ->
            when (change.operation) {
                TUPLE_OPERATION_WRITE -> tuples + Tuple(change.tuple_key, change.timestamp)
                TUPLE_OPERATION_DELETE -> tuples.filterNot { it.key == change.tuple_key }
            }
        }

    fun evaluation(id: AuthorizationModelId?): Evaluation? {
        val model = (if (id == null) models.lastOrNull() else models.find { it.id == id }) ?: return null
        return Evaluation(model.type_definitions, tuples.map { it.key })
    }
}
