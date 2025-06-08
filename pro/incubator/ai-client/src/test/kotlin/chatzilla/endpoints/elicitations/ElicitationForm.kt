package chatzilla.endpoints.elicitations

import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.template.ViewModel

data class ElicitationForm(val request: ElicitationRequest) : ViewModel {
    val id = request.progressToken!!
    val message = request.message
}
