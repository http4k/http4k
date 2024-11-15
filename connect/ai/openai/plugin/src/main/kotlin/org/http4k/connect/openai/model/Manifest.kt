package org.http4k.connect.openai.model

import org.http4k.core.Uri

internal data class Manifest(
    val name_for_human: String,
    val description_for_human: String,
    val name_for_model: String,
    val description_for_model: String,
    val api: Api,
    val auth: Map<String, Any>,
    val contact_email: Email,
    val logo_url: Uri,
    val legal_info_url: Uri
) {
    val schema_version = "v1"
}

