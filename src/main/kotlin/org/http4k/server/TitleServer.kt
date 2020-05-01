package org.http4k.server

import org.http4k.endpoints.EchoMessage
import org.http4k.endpoints.GetBasepathNometa
import org.http4k.endpoints.GetBasepathProduces_and_consumes
import org.http4k.endpoints.PostBasepathAnd_auth
import org.http4k.endpoints.PostBasepathBasic_auth
import org.http4k.endpoints.PostBasepathBearer_auth
import org.http4k.endpoints.PostBasepathBody_auto_schema
import org.http4k.endpoints.PostBasepathBody_auto_schema_multiple_request_schemas
import org.http4k.endpoints.PostBasepathBody_auto_schema_multiple_response_schemas
import org.http4k.endpoints.PostBasepathBody_auto_schema_name_definition_id
import org.http4k.endpoints.PostBasepathBody_form
import org.http4k.endpoints.PostBasepathBody_json_list_schema
import org.http4k.endpoints.PostBasepathBody_json_noschema
import org.http4k.endpoints.PostBasepathBody_json_response
import org.http4k.endpoints.PostBasepathBody_json_schema
import org.http4k.endpoints.PostBasepathBody_string
import org.http4k.endpoints.PostBasepathCookies
import org.http4k.endpoints.PostBasepathHeaders
import org.http4k.endpoints.PostBasepathOauth2_auth
import org.http4k.endpoints.PostBasepathOr_auth
import org.http4k.endpoints.PostBasepathPaths_firstName_Bertrand_age
import org.http4k.endpoints.PostBasepathQueries
import org.http4k.endpoints.PostBasepathReturning
import org.http4k.endpoints.PutBasepathBody_auto_map
import org.http4k.endpoints.PutBasepathBody_auto_schema
import org.http4k.endpoints.PutBasepathMultipart_fields
import org.http4k.routing.routes

object TitleServer {
  operator fun invoke() = routes(
  EchoMessage()
  , GetBasepathNometa()
  , GetBasepathProduces_and_consumes()
  , PostBasepathAnd_auth()
  , PostBasepathBasic_auth()
  , PostBasepathBearer_auth()
  , PostBasepathBody_auto_schema()
  , PostBasepathBody_auto_schema_multiple_request_schemas()
  , PostBasepathBody_auto_schema_multiple_response_schemas()
  , PostBasepathBody_auto_schema_name_definition_id()
  , PostBasepathBody_form()
  , PostBasepathBody_json_list_schema()
  , PostBasepathBody_json_noschema()
  , PostBasepathBody_json_response()
  , PostBasepathBody_json_schema()
  , PostBasepathBody_string()
  , PostBasepathCookies()
  , PostBasepathHeaders()
  , PostBasepathOauth2_auth()
  , PostBasepathOr_auth()
  , PostBasepathPaths_firstName_Bertrand_age()
  , PostBasepathQueries()
  , PostBasepathReturning()
  , PutBasepathBody_auto_map()
  , PutBasepathBody_auto_schema()
  , PutBasepathMultipart_fields()
  )
}
