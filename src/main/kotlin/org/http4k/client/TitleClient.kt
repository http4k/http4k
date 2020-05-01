package org.http4k.client

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response

class TitleClient(
  private val httpHandler: HttpHandler
) {
  fun postBasepathAnd_auth(): Response {
    return httpHandler(Request(Method.POST, "/basepath/and_auth"))
  }

  fun postBasepathBasic_auth(): Response {
    return httpHandler(Request(Method.POST, "/basepath/basic_auth"))
  }

  fun postBasepathBearer_auth(): Response {
    return httpHandler(Request(Method.POST, "/basepath/bearer_auth"))
  }

  fun putBasepathBody_auto_map(): Response {
    return httpHandler(Request(Method.PUT, "/basepath/body_auto_map"))
  }

  fun postBasepathBody_auto_schema(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_auto_schema"))
  }

  fun putBasepathBody_auto_schema(): Response {
    return httpHandler(Request(Method.PUT, "/basepath/body_auto_schema"))
  }

  fun postBasepathBody_auto_schema_multiple_request_schemas(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_auto_schema_multiple_request_schemas"))
  }

  fun postBasepathBody_auto_schema_multiple_response_schemas(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_auto_schema_multiple_response_schemas"))
  }

  fun postBasepathBody_auto_schema_name_definition_id(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_auto_schema_name_definition_id"))
  }

  fun postBasepathBody_form(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_form"))
  }

  fun postBasepathBody_json_list_schema(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_json_list_schema"))
  }

  fun postBasepathBody_json_noschema(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_json_noschema"))
  }

  fun postBasepathBody_json_response(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_json_response"))
  }

  fun postBasepathBody_json_schema(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_json_schema"))
  }

  fun postBasepathBody_string(): Response {
    return httpHandler(Request(Method.POST, "/basepath/body_string"))
  }

  fun postBasepathCookies(): Response {
    return httpHandler(Request(Method.POST, "/basepath/cookies"))
  }

  fun echoMessage(): Response {
    return httpHandler(Request(Method.GET, "/basepath/descriptions"))
  }

  fun postBasepathHeaders(): Response {
    return httpHandler(Request(Method.POST, "/basepath/headers"))
  }

  fun putBasepathMultipart_fields(): Response {
    return httpHandler(Request(Method.PUT, "/basepath/multipart_fields"))
  }

  fun getBasepathNometa(): Response {
    return httpHandler(Request(Method.GET, "/basepath/nometa"))
  }

  fun postBasepathOauth2_auth(): Response {
    return httpHandler(Request(Method.POST, "/basepath/oauth2_auth"))
  }

  fun postBasepathOr_auth(): Response {
    return httpHandler(Request(Method.POST, "/basepath/or_auth"))
  }

  fun postBasepathPaths_firstName_Bertrand_age(): Response {
    return httpHandler(Request(Method.POST, "/basepath/paths/{firstName}/bertrand/{age}"))
  }

  fun getBasepathProduces_and_consumes(): Response {
    return httpHandler(Request(Method.GET, "/basepath/produces_and_consumes"))
  }

  fun postBasepathQueries(): Response {
    return httpHandler(Request(Method.POST, "/basepath/queries"))
  }

  fun postBasepathReturning(): Response {
    return httpHandler(Request(Method.POST, "/basepath/returning"))
  }
}
